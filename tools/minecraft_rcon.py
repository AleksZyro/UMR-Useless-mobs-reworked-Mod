#!/usr/bin/env python3
"""Minimal Minecraft RCON client for local UMR runtime QA."""

from __future__ import annotations

import argparse
import os
import socket
import struct


def _read_exact(connection: socket.socket, length: int) -> bytes:
    chunks = []
    remaining = length
    while remaining:
        chunk = connection.recv(remaining)
        if not chunk:
            raise ConnectionError("RCON connection closed before the packet was complete")
        chunks.append(chunk)
        remaining -= len(chunk)
    return b"".join(chunks)


def encode_packet(request_id: int, packet_type: int, payload: str) -> bytes:
    body = struct.pack("<ii", request_id, packet_type) + payload.encode("utf-8") + b"\x00\x00"
    return struct.pack("<i", len(body)) + body


def receive_packet(connection: socket.socket) -> tuple[int, int, str]:
    length = struct.unpack("<i", _read_exact(connection, 4))[0]
    if length < 10 or length > 4 * 1024 * 1024:
        raise ValueError(f"Invalid RCON packet length: {length}")
    body = _read_exact(connection, length)
    request_id, packet_type = struct.unpack("<ii", body[:8])
    return request_id, packet_type, body[8:-2].decode("utf-8", errors="replace")


class MinecraftRcon:
    def __init__(self, host: str, port: int, password: str, timeout: float = 180.0):
        self._connection = socket.create_connection((host, port), timeout=timeout)
        self._connection.settimeout(timeout)
        self._authenticate(password)

    def _authenticate(self, password: str) -> None:
        self._connection.sendall(encode_packet(1, 3, password))
        request_id, _, _ = receive_packet(self._connection)
        if request_id == -1:
            raise PermissionError("RCON authentication failed")
        if request_id != 1:
            raise ConnectionError(f"Unexpected RCON authentication response id: {request_id}")

    def command(self, command: str) -> str:
        self._connection.sendall(encode_packet(2, 2, command))
        request_id, _, payload = receive_packet(self._connection)
        if request_id != 2:
            raise ConnectionError(f"Unexpected RCON command response id: {request_id}")
        return payload

    def close(self) -> None:
        self._connection.close()

    def __enter__(self):
        return self

    def __exit__(self, *_):
        self.close()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("command")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=25575)
    parser.add_argument("--timeout", type=float, default=180.0)
    args = parser.parse_args()
    password = os.environ.get("UMR_RCON_PASSWORD")
    if not password:
        raise SystemExit("UMR_RCON_PASSWORD is required")
    with MinecraftRcon(args.host, args.port, password, args.timeout) as client:
        print(client.command(args.command))


if __name__ == "__main__":
    main()
