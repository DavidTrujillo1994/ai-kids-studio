extends Node2D
## Lluvia y nieve. La posición sale solo del tiempo: el mismo cuadro siempre se ve igual.
const COUNT := 130

var kind := ""
var now := 0.0


func step(t: float) -> void:
	now = t
	visible = kind == "rain" or kind == "snow"
	if visible:
		queue_redraw()


func _draw() -> void:
	var rng := RandomNumberGenerator.new()
	rng.seed = 9
	for i in range(COUNT):
		var x := rng.randf_range(-100, 1380)
		var y := rng.randf_range(0, 760)
		var speed := rng.randf_range(0.8, 1.2)
		if kind == "rain":
			var p := Vector2(x - fmod(now * 160.0 * speed, 200.0) + 100.0, fmod(y + now * 900.0 * speed, 760.0) - 30.0)
			draw_line(p, p + Vector2(-5, 26), Color(0.85, 0.93, 1.0, 0.55), 2.2)
		else:
			var p := Vector2(x + sin(now * 1.3 + i) * 26.0, fmod(y + now * 85.0 * speed, 760.0) - 20.0)
			draw_circle(p, 2.0 + 2.6 * speed, Color(1, 1, 1, 0.85))
