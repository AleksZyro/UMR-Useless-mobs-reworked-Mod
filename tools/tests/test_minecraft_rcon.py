import socket
import struct
import threading

from tools.minecraft_rcon import MinecraftRcon, encode_packet, receive_packet


def test_packet_round_trip_handles_fragmented_reads():
    client, server = socket.socketpair()
    packet = encode_packet(7, 0, "biome found")

    def send_fragments():
        server.sendall(packet[:3])
        server.sendall(packet[3:])
        server.close()

    thread = threading.Thread(target=send_fragments)
    thread.start()
    assert receive_packet(client) == (7, 0, "biome found")
    thread.join()
    client.close()


def test_client_authenticates_and_runs_command(monkeypatch):
    client_socket, server_socket = socket.socketpair()

    def fake_create_connection(*_args, **_kwargs):
        return client_socket

    def read_request():
        length = struct.unpack("<i", server_socket.recv(4))[0]
        body = server_socket.recv(length)
        return struct.unpack("<ii", body[:8]), body[8:-2].decode()

    def server():
        assert read_request() == ((1, 3), "test-password")
        server_socket.sendall(encode_packet(1, 2, ""))
        assert read_request() == ((2, 2), "locate biome usless_mobs:deep_ocean")
        server_socket.sendall(encode_packet(2, 0, "The nearest biome is at [100, 40, -200]"))
        server_socket.close()

    monkeypatch.setattr(socket, "create_connection", fake_create_connection)
    thread = threading.Thread(target=server)
    thread.start()
    with MinecraftRcon("127.0.0.1", 25575, "test-password", timeout=1) as rcon:
        assert rcon.command("locate biome usless_mobs:deep_ocean") == (
            "The nearest biome is at [100, 40, -200]"
        )
    thread.join()
