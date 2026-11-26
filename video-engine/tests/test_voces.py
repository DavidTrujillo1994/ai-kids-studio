"""Ejecutar: .venv\\Scripts\\python.exe -m unittest discover video-engine/tests"""
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "scripts"))
import producir
import voz_expresiva


class Plain(unittest.TestCase):
    def test_digits_are_compared_as_words(self):
        self.assertEqual(producir.plain("Uno, dos, tres... ¡Allá voy!"), producir.plain("1, 2, 3, allá voy"))

    def test_numbers(self):
        self.assertEqual([producir.words(n) for n in (0, 16, 21, 30, 42, 100, 115, 999)],
                         ["cero", "dieciséis", "veintiuno", "treinta", "cuarenta y dos", "cien", "ciento quince", "novecientos noventa y nueve"])


class Expressive(unittest.TestCase):
    """Los modelos se simulan: aquí se prueba qué se guarda, qué se repite y qué se rechaza."""

    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        ep = Path(tmp.name)
        (ep / "audio").mkdir()
        profile = {"engine": "chatterbox", "reference": "r.wav", "seed": 1, "exaggeration": 1, "cfg": 0.4, "pitch": 1}
        self.lines = [producir.Line(ep, "luna", text, profile) for text in ("Hola amigos", "Vamos al lago")]
        self.voice = mock.Mock()
        self.voice.tokens.return_value = [1]
        self.voice.sound.return_value = np.zeros(10, np.float32)

        def finish(line):
            for f in (line.wav, line.cues):
                f.write_text("x")
            line.stamp.write_text(line.key, encoding="utf-8")
        for target, name, value in ((voz_expresiva, "Voice", mock.Mock(return_value=self.voice)), (voz_expresiva, "write_wav", mock.Mock()),
                                    (producir.Line, "finish", finish), (producir, "print", mock.Mock())):
            patch = mock.patch.object(target, name, value, create=True)
            patch.start()
            self.addCleanup(patch.stop)

    def test_only_the_bad_take_is_repeated(self):
        with mock.patch.object(producir, "heard", side_effect=[["hola amigos", "xyz"], ["vamos al lago"]]):
            producir.make_voices(self.lines)
        self.assertEqual(self.voice.sound.call_count, 3)
        self.assertTrue(all(ln.ready() for ln in self.lines))

    def test_a_bad_phrase_does_not_lose_the_good_ones(self):
        with mock.patch.object(producir, "heard", side_effect=[["hola amigos", "xyz"], ["xyz"], ["xyz"]]):
            with self.assertRaises(SystemExit) as error:
                producir.make_voices(self.lines)
        self.assertIn("Vamos al lago", str(error.exception))
        self.assertEqual([ln.ready() for ln in self.lines], [True, False])

    def test_ready_and_repeated_phrases_are_made_once(self):
        with mock.patch.object(producir, "heard", side_effect=[["hola amigos"]]):
            producir.make_voices([self.lines[0], self.lines[0]])
            producir.make_voices([self.lines[0]])
        self.assertEqual(self.voice.sound.call_count, 1)


if __name__ == "__main__":
    unittest.main()
