extends Node2D
## Reproduce una escena compilada (escena.json). Uso:
## godot --path . --write-movie out.avi --fixed-fps 24 -- --scene=RUTA --media=RUTA
const Rig = preload("res://rig.gd")
const Place = preload("res://place.gd")
const Prop = preload("res://prop.gd")
const Weather = preload("res://weather.gd")

var scene: Dictionary = {}
var media := ""
var frame := 0
var fps := 24.0
var stage := Node2D.new()
var world := Node2D.new()
var weather := Weather.new()
var curtain := ColorRect.new()
var places: Array = []
var actors := {}
var props: Array = []
var next_event := 0
var current := -1


func _ready() -> void:
	var args := {}
	for a in OS.get_cmdline_user_args():
		var kv := a.trim_prefix("--").split("=", true, 1)
		if kv.size() == 2:
			args[kv[0]] = kv[1]
	if not (args.has("scene") and args.has("media")):
		push_error("Faltan --scene y --media")
		get_tree().quit(2)
		return
	media = args["media"]
	scene = JSON.parse_string(FileAccess.get_file_as_string(args["scene"]))
	fps = scene["fps"]
	add_child(stage)
	for s in scene["scenes"]:
		var place := Place.new()
		stage.add_child(place.back)
		place.build("%s/environments/%s" % [media, s["environment"]], s["ambience"])
		places.append(place)
	stage.add_child(world)
	for place in places:
		stage.add_child(place.front)
	_build_actors()
	for p in scene["props"]:
		var prop := Prop.new()
		world.add_child(prop)
		prop.setup("%s/props/%s" % [media, p["prop"]], p)
		props.append(prop)
	weather.z_index = 3500
	add_child(weather)
	curtain.color = Color.BLACK
	curtain.size = Vector2(1280, 720)
	curtain.z_index = 4000
	add_child(curtain)
	_update(0.0)


func _build_actors() -> void:
	var anims: Dictionary = JSON.parse_string(FileAccess.get_file_as_string(media + "/characters/animaciones.json"))
	var z := 100
	for c in scene["characters"]:
		var rig := Rig.new()
		world.add_child(rig)
		rig.setup("%s/characters/%s" % [media, c["id"]], anims, z)
		rig.position.x = c["x"]
		actors[c["id"]] = rig
		z += 100


func _process(_delta: float) -> void:
	if scene.is_empty():
		return
	_update(frame / fps)
	frame += 1


func _update(t: float) -> void:
	_enter_scene(t)
	_apply_events(t)
	_move(t)
	var speaker = _speak(t)
	var attention: Node2D = null
	for p in props:
		p.step(t, current)
		if p.visible:
			attention = p
	for id in actors:
		var actor = actors[id]
		actor.look = Vector2.INF
		if speaker != null and actor != speaker:
			actor.look = speaker.head_position()
		elif attention != null:
			actor.look = attention.global_position
		actor.step(t, 1.0 / fps, places[current].ground_y)
	_update_camera(t)
	weather.step(t)
	curtain.modulate.a = _curtain(t)


func _enter_scene(t: float) -> void:
	var index := 0
	for i in range(scene["scenes"].size()):
		if t >= scene["scenes"][i]["t"]:
			index = i
	if index == current:
		return
	current = index
	for i in range(places.size()):
		places[i].show(i == index)
	world.modulate = places[index].tint.lerp(Color.WHITE, 0.5)
	weather.kind = places[index].weather
	var positions: Dictionary = scene["scenes"][index]["positions"]
	for id in positions:
		actors[id].position.x = positions[id]
		actors[id].lift = 0.0
		actors[id].settle()


func _apply_events(t: float) -> void:
	var events: Array = scene["events"]
	while next_event < events.size() and events[next_event]["t"] <= t:
		var e: Dictionary = events[next_event]
		var actor = actors[e["character"]]
		if e.has("animation"):
			actor.play(e["animation"])
		if e.has("expression"):
			actor.expression = e["expression"]
		if e.has("facing"):
			actor.face(e["facing"])
		if e.has("lift"):
			actor.lift_target = e["lift"]
		next_event += 1


func _move(t: float) -> void:
	for m in scene["moves"]:
		if t >= m["start"] and t < m["end"] + 0.1:
			var k := clampf((t - m["start"]) / (m["end"] - m["start"]), 0.0, 1.0)
			actors[m["character"]].position.x = lerpf(m["from"], m["to"], k)


func _speak(t: float):
	for id in actors:
		actors[id].mouth = ""
	for d in scene["dialogues"]:
		if t >= d["start"] and t < d["end"]:
			var actor = actors[d["character"]]
			for cue in d["cues"]:
				if t - d["start"] >= cue["start"] and t - d["start"] < cue["end"]:
					actor.mouth = cue["value"]
			return actor
	return null


func _curtain(t: float) -> float:
	var a := 0.0
	for i in range(1, scene["scenes"].size()):
		var d := absf(t - scene["scenes"][i]["t"])
		a = maxf(a, 1.0 - d / 0.35)
	return clampf(a, 0.0, 1.0)


func _update_camera(t: float) -> void:
	var keys: Array = scene["camera"]
	var cx: float = keys[-1]["x"]
	var zoom: float = keys[-1]["zoom"]
	for i in range(keys.size() - 1):
		if t < keys[i + 1]["t"]:
			var k := smoothstep(keys[i]["t"], keys[i + 1]["t"], t)
			cx = lerpf(keys[i]["x"], keys[i + 1]["x"], k)
			zoom = lerpf(keys[i]["zoom"], keys[i + 1]["zoom"], k)
			break
	stage.scale = Vector2(zoom, zoom)
	stage.position = Vector2(640, 600) * (1.0 - zoom)
	world.position.x = -cx
	places[current].scroll(cx)
