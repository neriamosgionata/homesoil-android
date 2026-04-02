package com.homesoil.app.util

data class ScriptError(
    val line: Int,
    val message: String,
    val argErrors: List<ArgError> = emptyList()
)

data class ArgError(
    val arg: Int,
    val message: String
)

private enum class SymbolType { FUNCTION, INSTRUCTION, INSTRUCTION_BLOCK, MAIN }
private enum class ArgType { STRING, NUMBER, BOOLEAN, VARIABLE, ANY }

private data class CommandDef(
    val type: SymbolType,
    val argCount: Int,
    val expectedArgs: List<List<ArgType>>
)

object ScriptValidator {

    private val language = mapOf(
        "ACTIVATE" to CommandDef(SymbolType.FUNCTION, 1, listOf(listOf(ArgType.NUMBER, ArgType.VARIABLE))),
        "DEACTIVATE" to CommandDef(SymbolType.FUNCTION, 1, listOf(listOf(ArgType.NUMBER, ArgType.VARIABLE))),
        "PULSE" to CommandDef(SymbolType.FUNCTION, 1, listOf(listOf(ArgType.NUMBER, ArgType.VARIABLE))),
        "READ" to CommandDef(SymbolType.FUNCTION, 1, listOf(listOf(ArgType.NUMBER, ArgType.VARIABLE))),
        "SEND_TO_DASHBOARD" to CommandDef(SymbolType.FUNCTION, 1, listOf(listOf(ArgType.NUMBER, ArgType.VARIABLE, ArgType.STRING))),
        "SET" to CommandDef(SymbolType.FUNCTION, 2, listOf(listOf(ArgType.STRING, ArgType.NUMBER), listOf(ArgType.ANY))),
        "UNSET" to CommandDef(SymbolType.FUNCTION, 1, listOf(listOf(ArgType.STRING, ArgType.NUMBER))),
        "ADD" to CommandDef(SymbolType.FUNCTION, 2, listOf(listOf(ArgType.NUMBER, ArgType.VARIABLE), listOf(ArgType.NUMBER, ArgType.VARIABLE))),
        "SUBTRACT" to CommandDef(SymbolType.FUNCTION, 2, listOf(listOf(ArgType.NUMBER, ArgType.VARIABLE), listOf(ArgType.NUMBER, ArgType.VARIABLE))),
        "MULTIPLY" to CommandDef(SymbolType.FUNCTION, 2, listOf(listOf(ArgType.NUMBER, ArgType.VARIABLE), listOf(ArgType.NUMBER, ArgType.VARIABLE))),
        "DIVIDE" to CommandDef(SymbolType.FUNCTION, 2, listOf(listOf(ArgType.NUMBER, ArgType.VARIABLE), listOf(ArgType.NUMBER, ArgType.VARIABLE))),
        "MODULO" to CommandDef(SymbolType.FUNCTION, 2, listOf(listOf(ArgType.NUMBER, ArgType.VARIABLE), listOf(ArgType.NUMBER, ArgType.VARIABLE))),
        "DELAY" to CommandDef(SymbolType.FUNCTION, 1, listOf(listOf(ArgType.NUMBER))),
        "IF" to CommandDef(SymbolType.INSTRUCTION, 1, listOf(listOf(ArgType.ANY))),
        "WHILE" to CommandDef(SymbolType.INSTRUCTION, 1, listOf(listOf(ArgType.ANY))),
        "LOOP" to CommandDef(SymbolType.INSTRUCTION, 0, emptyList()),
        "BREAK" to CommandDef(SymbolType.INSTRUCTION, 0, emptyList()),
        "CONTINUE" to CommandDef(SymbolType.INSTRUCTION, 0, emptyList()),
        "THEN" to CommandDef(SymbolType.INSTRUCTION_BLOCK, 0, emptyList()),
        "END" to CommandDef(SymbolType.INSTRUCTION_BLOCK, 0, emptyList()),
        "RUN" to CommandDef(SymbolType.MAIN, 0, emptyList()),
        "STOP" to CommandDef(SymbolType.MAIN, 0, emptyList())
    )

    val KEYWORDS: List<String> = language.entries
        .filter { it.value.type == SymbolType.FUNCTION || it.value.type == SymbolType.INSTRUCTION }
        .map { it.key }

    fun validate(code: String): List<ScriptError> {
        val lines = code.split("\n")
        val errors = mutableListOf<ScriptError>()
        var missingRun = true
        var missingStop = true
        var openBlocks = 0
        var waitingForThen = false

        for (i in lines.indices) {
            val tokens = lines[i].replace("\t", "").trim().split(" ").filter { it.isNotEmpty() }
            if (tokens.isEmpty()) continue

            val command = tokens[0]

            if (command == "RUN" && i == 0) {
                missingRun = false
                continue
            }
            if (command == "STOP" && i == lines.lastIndex && i != 0) {
                missingStop = false
                continue
            }

            val def = language[command]
            if (def == null) {
                errors.add(ScriptError(i + 1, "Unknown command $command"))
                continue
            }

            if (def.type == SymbolType.MAIN) continue

            if (def.type == SymbolType.INSTRUCTION_BLOCK) {
                if (command == "THEN") {
                    if (!waitingForThen) {
                        errors.add(ScriptError(i + 1, "Keyword THEN not expected here"))
                        continue
                    }
                    waitingForThen = false
                    openBlocks++
                    continue
                }
                if (command == "END") {
                    if (waitingForThen || openBlocks <= 0) {
                        errors.add(ScriptError(i + 1, "Keyword END not expected here"))
                        continue
                    }
                    openBlocks--
                    continue
                }
            }

            val args = tokens.drop(1).toMutableList()

            if (def.type == SymbolType.INSTRUCTION) {
                if (args.contains("THEN")) {
                    args.removeAt(args.indexOf("THEN"))
                    openBlocks++
                } else {
                    waitingForThen = true
                }

                if (args.size < def.argCount) {
                    errors.add(ScriptError(i + 1, "Instruction $command expects ${def.argCount} arguments, ${args.size} given"))
                } else {
                    val argErrors = checkArgs(command, args, def.expectedArgs)
                    if (argErrors.isNotEmpty()) {
                        errors.add(ScriptError(i + 1, "Instruction $command has invalid arguments", argErrors))
                    }
                }
                continue
            }

            if (def.type == SymbolType.FUNCTION) {
                if (args.size < def.argCount) {
                    errors.add(ScriptError(i + 1, "Function $command expects ${def.argCount} arguments, ${args.size} given"))
                } else {
                    val argErrors = checkArgs(command, args, def.expectedArgs)
                    if (argErrors.isNotEmpty()) {
                        errors.add(ScriptError(i + 1, "Function $command has invalid arguments", argErrors))
                    }
                }
                continue
            }
        }

        if (missingRun) {
            errors.add(0, ScriptError(1, "Missing RUN instruction at the beginning of the script"))
        }
        if (missingStop) {
            errors.add(ScriptError(lines.size, "Missing STOP instruction at the end of the script"))
        }

        return errors
    }

    private fun parseArgType(arg: String): ArgType = when {
        arg == "true" || arg == "false" -> ArgType.BOOLEAN
        arg.startsWith("\"") || arg.endsWith("\"") -> ArgType.STRING
        arg.contains("$") -> ArgType.VARIABLE
        arg.contains(".") || arg.matches(Regex("^[0-9]+$")) -> ArgType.NUMBER
        else -> ArgType.ANY
    }

    private fun checkArgs(command: String, args: List<String>, expected: List<List<ArgType>>): List<ArgError> {
        val errors = mutableListOf<ArgError>()
        for (i in args.indices) {
            if (i >= expected.size) break
            val allowed = expected[i]
            if (allowed.contains(ArgType.ANY)) continue

            val actual = parseArgType(args[i])
            if (actual == ArgType.ANY) {
                errors.add(ArgError(i + 1, "Argument ${i + 1} of command $command is an unknown type"))
                continue
            }

            if (!args[i].matches(Regex("^\"(.)+(\")\$")) && args[i].contains("\"") && allowed.contains(ArgType.STRING)) {
                errors.add(ArgError(i + 1, "Argument ${i + 1} of command $command is an invalid string"))
                continue
            }

            if (!allowed.contains(actual)) {
                val expectedStr = allowed.joinToString(" or ") { it.name.lowercase() }
                errors.add(ArgError(i + 1, "Argument ${i + 1} of command $command expects $expectedStr, ${actual.name.lowercase()} given"))
            }
        }
        return errors
    }
}
