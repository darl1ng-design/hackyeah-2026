"""Result monad: Ok | Err with map/bind for composition (pattern from multimodal-rag)."""

from collections.abc import Callable
from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class Ok[T]:
    value: T

    def map[U](self, f: Callable[[T], U]) -> "Ok[U]":
        return Ok(f(self.value))

    def bind[U, E](self, f: "Callable[[T], Result[U, E]]") -> "Result[U, E]":
        return f(self.value)


@dataclass(frozen=True, slots=True)
class Err[E]:
    error: E

    def map(self, _f: Callable) -> "Err[E]":
        return self

    def bind(self, _f: Callable) -> "Err[E]":
        return self


type Result[T, E] = Ok[T] | Err[E]


def attempt[T](f: Callable[[], T], stage: str) -> Result[T, str]:
    """Lift a raising I/O call into a Result. Only for the shell; the core never raises."""
    try:
        return Ok(f())
    except Exception as e:  # ponytail: broad on purpose, this IS the I/O boundary
        return Err(f"{stage}: {e}")
