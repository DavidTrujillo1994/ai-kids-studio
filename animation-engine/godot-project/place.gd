extends RefCounted
## Un escenario: capas de fondo y de frente con paralaje, más su ambiente (hora y clima).
const Svg = preload("res://svg.gd")
const Stars = preload("res://stars.gd")

var back := Node2D.new()
var front := Node2D.new()
var layers: Array = []
var ground_y := 622.0
var tint := Color.WHITE
var weather := ""


func build(path: String, ambience: String) -> void:
	var env: Dictionary = JSON.parse_string(FileAccess.get_file_as_string(path + "/escenario.json"))
	var all: Dictionary = JSON.parse_string(FileAccess.get_file_as_string(path.get_base_dir() + "/ambientes.json"))
	var look: Dictionary = all.get(ambience, {})
	var indoors: bool = env.get("interior", false)
	tint = Color(look.get("tint", "#ffffff"))
	if indoors:
		tint = tint.lerp(Color.WHITE, 0.45)
	else:
		weather = look.get("particles", "")
	back.modulate = tint
	front.modulate = tint
	ground_y = env["groundY"]
	for i in range(env["layers"].size()):
		_layer(back, path, env["layers"][i], env["originX"], -50)
		if i == 0 and weather == "stars":
			var stars := Stars.new()
			stars.z_index = -50
			back.add_child(stars)
	for l in env["foreground"]:
		_layer(front, path, l, env["originX"], 3000)


func show(on: bool) -> void:
	back.visible = on
	front.visible = on


func scroll(cx: float) -> void:
	for l in layers:
		l["node"].position.x = -cx * l["factor"]


func _layer(parent: Node2D, path: String, l: Array, origin: float, z: int) -> void:
	var n := Node2D.new()
	var s := Sprite2D.new()
	s.texture = Svg.texture(path + "/" + l[0], 1.0)
	s.centered = false
	s.position.x = origin
	s.z_index = z
	n.add_child(s)
	parent.add_child(n)
	layers.append({"node": n, "factor": l[1]})
