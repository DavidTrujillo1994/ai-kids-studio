extends RefCounted
## Carga SVG en tiempo de ejecución (sin importarlos al proyecto) y los guarda en caché.
static var cache := {}

static func texture(path: String, scale: float) -> ImageTexture:
	var key := "%s@%s" % [path, scale]
	if not cache.has(key):
		var text := FileAccess.get_file_as_string(path)
		var img := Image.new()
		if text.is_empty() or img.load_svg_from_string(text, scale) != OK:
			push_error("SVG inválido: " + path)
			return null
		cache[key] = ImageTexture.create_from_image(img)
	return cache[key]
