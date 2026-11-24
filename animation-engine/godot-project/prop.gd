extends Node2D
## Objeto que sigue un recorrido. Su forma de moverse la define prop.json.
const Svg = preload("res://svg.gd")

var path: Array
var scene_index := 0
var motion := "flap"
var wings: Array = []
var body := Sprite2D.new()


func setup(dir: String, data: Dictionary) -> void:
	z_index = 2000
	path = data["path"]
	scene_index = data["scene"]
	var info = JSON.parse_string(FileAccess.get_file_as_string(dir + "/prop.json"))
	if info is Dictionary:
		motion = info.get("motion", motion)
	if motion == "flap":
		for side in [-1.0, 1.0]:
			var s := Sprite2D.new()
			s.texture = Svg.texture(dir + "/ala.svg", 2.0)
			s.offset = Vector2(s.texture.get_width() / 2.0, 0)
			s.scale = Vector2(0.5 * side, 0.5)
			add_child(s)
			wings.append(s)
	body.texture = Svg.texture(dir + "/cuerpo.svg", 2.0)
	body.scale = Vector2(0.5, 0.5)
	add_child(body)


func step(t: float, current_scene: int) -> void:
	visible = current_scene == scene_index and t >= path[0][0] and t <= path[-1][0]
	if not visible:
		return
	for i in range(path.size() - 1):
		if t >= path[i][0] and t <= path[i + 1][0]:
			var k: float = inverse_lerp(path[i][0], path[i + 1][0], t)
			if motion != "bounce":
				k = smoothstep(0.0, 1.0, k)
			position = Vector2(path[i][1], path[i][2]).lerp(Vector2(path[i + 1][1], path[i + 1][2]), k)
	match motion:
		"flap":
			position += Vector2(sin(t * 2.3) * 12.0, sin(t * 5.0) * 14.0)
			var flap := 0.12 + 0.38 * absf(sin(t * 11.0))
			wings[0].scale.x = -flap
			wings[1].scale.x = flap
		"bounce":
			position.y -= absf(sin(t * 4.2)) * 130.0
			body.rotation = t * 4.0
		"float":
			position += Vector2(sin(t * 1.4) * 16.0, sin(t * 1.9) * 12.0)
			body.rotation = sin(t * 1.4) * 0.12
		"sway":
			position += Vector2(sin(t * 2.0) * 34.0, 0)
			body.rotation = sin(t * 2.0 + 0.8) * 0.5
		"spin":
			body.rotation = t * 1.6
			body.scale = Vector2.ONE * (0.5 + 0.06 * sin(t * 6.0))
