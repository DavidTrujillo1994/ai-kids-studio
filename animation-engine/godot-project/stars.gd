extends Node2D
## Estrellas fijas para la noche; van justo encima del cielo.


func _draw() -> void:
	var rng := RandomNumberGenerator.new()
	rng.seed = 5
	for i in range(90):
		var p := Vector2(rng.randf_range(-100, 1400), rng.randf_range(10, 330))
		draw_circle(p, rng.randf_range(1.2, 3.2), Color(1.6, 1.6, 1.3, rng.randf_range(0.5, 1.0)))
