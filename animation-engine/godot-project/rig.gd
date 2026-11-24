extends Node2D
## Personaje con esqueleto Bone2D construido desde rig.json. El cuerpo lo mueve un
## AnimationPlayer; la cara (ojos, mirada, cejas, boca) es una capa aparte, y las
## piezas sueltas (orejas, pañuelo, mechón) cuelgan con un resorte.
const Svg = preload("res://svg.gd")
const Q := 2.0
const BLEND := 0.18
const GAZE := 7.0
const CLOSED := ["happy", "closed", "sleepy"]
const LIFT_SPEED := 3.5

var dir: String
var bones := {}
var bone_paths := {}
var sprites := {}
var mirror: Array = []
var dangles := {}
var springs := {}
var player := AnimationPlayer.new()
var expression := "normal"
var mouth := ""
var look := Vector2.INF
var gaze := Vector2.ZERO
var face_scale: float
var base_hip: Vector2
var last_hip := 0.0
var last_x := 0.0
var started := false
var lift := 0.0
var lift_target := 0.0


func setup(character_dir: String, anims: Dictionary, z_base: int) -> void:
	dir = character_dir
	var rig: Dictionary = JSON.parse_string(FileAccess.get_file_as_string(dir + "/rig.json"))
	face_scale = rig["defaultScale"]
	mirror = rig.get("mirror", [])
	dangles = rig.get("dangles", {})
	scale = Vector2.ONE * face_scale
	var skeleton := Skeleton2D.new()
	skeleton.name = "Skeleton"
	add_child(skeleton)
	for b in rig["bones"]:
		var bone := Bone2D.new()
		bone.name = b[0]
		bone.position = Vector2(b[2], b[3])
		bone.rest = Transform2D(0.0, bone.position)
		bone.set_autocalculate_length_and_angle(false)
		bone.set_length(40)
		var parent: Node = skeleton if b[1] == "" else bones[b[1]]
		parent.add_child(bone)
		bones[b[0]] = bone
		var base: String = "Skeleton" if b[1] == "" else bone_paths[b[1]]
		bone_paths[b[0]] = base + "/" + b[0]
	base_hip = bones["hip"].position
	for s in rig["sprites"]:
		var sp := Sprite2D.new()
		sp.texture = Svg.texture(dir + "/" + s[2], Q)
		sp.scale = Vector2.ONE / Q
		sp.flip_h = s[0] in mirror
		sp.position = Vector2(s[3], s[4])
		sp.z_as_relative = false
		sp.z_index = z_base + int(s[5])
		var holder: Node = self if s[1] == "" else bones[s[1]]
		holder.add_child(sp)
		sprites[s[0]] = sp
		sp.set_meta("home", sp.position)
	for n in dangles:
		springs[n] = Vector2.ZERO
	player.callback_mode_process = AnimationMixer.ANIMATION_CALLBACK_MODE_PROCESS_MANUAL
	add_child(player)
	player.root_node = player.get_path_to(self)
	var lib := AnimationLibrary.new()
	for n in anims:
		lib.add_animation(n, _build(anims[n]))
	player.add_animation_library("", lib)
	player.play("idle")
	player.seek(float(z_base % 7) * 0.27, true)  # cada personaje respira a su ritmo


## Todas las animaciones llevan pista para todos los huesos: ninguna deja restos de la anterior.
func _build(data: Dictionary) -> Animation:
	var a := Animation.new()
	a.length = data["length"]
	a.loop_mode = Animation.LOOP_LINEAR if data["loop"] else Animation.LOOP_NONE
	for n in bones:
		if dangles.has(n):
			continue
		var i := a.add_track(Animation.TYPE_VALUE)
		a.track_set_path(i, NodePath(bone_paths[n] + ":rotation"))
		a.track_set_interpolation_type(i, Animation.INTERPOLATION_CUBIC)
		for k in data["tracks"].get(n, [[0, 0]]):
			a.track_insert_key(i, k[0], deg_to_rad(k[1]))
	var h := a.add_track(Animation.TYPE_VALUE)
	a.track_set_path(h, NodePath(bone_paths["hip"] + ":position"))
	a.track_set_interpolation_type(h, Animation.INTERPOLATION_CUBIC)
	for k in data["tracks"].get("hip_y", [[0, 0]]):
		a.track_insert_key(h, k[0], base_hip + Vector2(0, k[1]))
	return a


func play(anim: String) -> void:
	if not player.has_animation(anim):
		push_error("Animación desconocida: " + anim)
	elif player.current_animation != anim:
		player.play(anim, BLEND)


func face(direction: float) -> void:
	scale.x = face_scale * (1.0 if direction >= 0.0 else -1.0)


## Tras un corte no hay inercia que arrastrar.
func settle() -> void:
	started = false
	lift_target = 0.0
	for n in springs:
		springs[n] = Vector2.ZERO


func head_position() -> Vector2:
	return bones["head"].to_global(Vector2(0, -110))


func step(t: float, delta: float, ground: float) -> void:
	player.advance(delta)
	lift = lerpf(lift, lift_target, clampf(delta * LIFT_SPEED, 0.0, 1.0))
	position.y = ground - lift
	sprites["sombra"].position.y = sprites["sombra"].get_meta("home").y + maxf(lift, 0.0) / face_scale
	_body(delta)
	_face(t)


func _body(delta: float) -> void:
	var hip: Vector2 = bones["hip"].position
	var vy := (hip.y - last_hip) / delta if started else 0.0
	var vx := (position.x - last_x) / delta if started else 0.0
	started = true
	last_hip = hip.y
	last_x = position.x
	# estirar y aplastar conservando el volumen
	var s := clampf(-vy / 7000.0, -0.07, 0.07) if vy < 0.0 else clampf(-vy / 9000.0, -0.05, 0.05)
	bones["torso"].scale = Vector2(1.0 - s, 1.0 + absf(s) if vy < 0.0 else 1.0 + s)
	for n in dangles:
		var a: Vector2 = springs[n]
		var force: float = dangles[n] * (vy * 0.11 - vx * signf(scale.x) * 0.045)
		var acc := force - 150.0 * a.x - 9.0 * a.y
		a.y += acc * delta
		a.x = clampf(a.x + a.y * delta, -0.6, 0.6)
		springs[n] = a
		bones[n].rotation = a.x
	if sprites.has("sombra"):
		var air := clampf((base_hip.y - hip.y + maxf(lift, 0.0)) / 140.0, 0.0, 1.0)
		sprites["sombra"].scale = Vector2.ONE / Q * (1.0 - 0.35 * air)


func _tex(sprite: String, file: String) -> void:
	sprites[sprite].texture = Svg.texture("%s/%s.svg" % [dir, file], Q)


func _face(t: float) -> void:
	var eyes := expression
	if not (eyes in CLOSED) and fmod(t + 1.3, 2.9) < 0.13:
		eyes = "closed"
	var target := Vector2(2.0, 0.0)
	if look != Vector2.INF:
		target = (bones["head"].to_local(look) - Vector2(14, -124)).normalized() * GAZE
	gaze = gaze.lerp(target, 0.25)
	for side in ["l", "r"]:
		_tex("eye_" + side, "ojos/" + eyes)
		var iris: Sprite2D = sprites["iris_" + side]
		iris.visible = not (eyes in CLOSED)
		iris.position = iris.get_meta("home") + gaze
		iris.scale = Vector2.ONE / Q * {"surprised": 0.82, "scared": 0.62}.get(eyes, 1.0)
		var lid: Sprite2D = sprites["lid_" + side]
		lid.visible = eyes in ["sad", "angry"] or (eyes == "picaro" and side == "r")
		if lid.visible:
			_tex("lid_" + side, "ojos/parpado-" + eyes)
			lid.flip_h = eyes in ["sad", "angry"] and side == ("l" if eyes == "sad" else "r")
		var brow: Sprite2D = sprites["brow_" + side]
		var lift: float = {"surprised": -16.0, "scared": -14.0, "happy": -8.0, "angry": 12.0, "picaro": -14.0 if side == "l" else 16.0}.get(expression, 0.0)
		brow.position = brow.get_meta("home") + Vector2(0, lift)
		var tilt: float = {"sad": -0.35, "scared": -0.3, "angry": 0.4, "picaro": -0.12}.get(expression, 0.0)
		brow.rotation = tilt if side == "l" else -tilt
	var m := mouth
	if m == "" or m == "X":
		m = "X" if expression == "normal" else expression
	_tex("mouth", "bocas/" + m)
