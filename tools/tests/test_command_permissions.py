from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
COMMAND_SOURCE = ROOT / "src/main/java/com/Momik/usless_mobs/command/UmrCommand.java"


def test_umr_mutating_commands_require_operator_permission():
    source = COMMAND_SOURCE.read_text(encoding="utf-8")

    assert "private static final int OPERATOR_PERMISSION_LEVEL = 2;" in source
    assert "private static boolean requiresOperator(CommandSourceStack source)" in source

    for command in ("disable", "enable", "clear", "debug"):
        command_start = source.index(f'Commands.literal("{command}")')
        command_end = source.find("\n                        .then", command_start + 1)
        command_block = source[command_start:] if command_end == -1 else source[command_start:command_end]
        assert ".requires(UmrCommand::requiresOperator)" in command_block, command


def test_umr_effect_list_remains_readable_without_operator_permission():
    source = COMMAND_SOURCE.read_text(encoding="utf-8")
    list_start = source.index('Commands.literal("list")')
    list_end = source.index('Commands.literal("clear")', list_start)

    assert ".requires(UmrCommand::requiresOperator)" not in source[list_start:list_end]
