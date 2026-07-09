package com.example.interpreter

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlin.random.Random

// Sealed class representing drawing commands
sealed class DrawCommand {
    data class Rect(val x: Float, val y: Float, val w: Float, val h: Float, val color: String) : DrawCommand()
    data class Circle(val x: Float, val y: Float, val r: Float, val color: String) : DrawCommand()
    data class Text(val text: String, val x: Float, val y: Float, val size: Float, val color: String) : DrawCommand()
}

// Representation of a Python function in our interpreter
data class PythonFunction(
    val name: String,
    val parameters: List<String>,
    val bodyLines: List<String>
)

class PythonInterpreter {
    // Variable scope
    val globals = mutableMapOf<String, Any?>()
    val functions = mutableMapOf<String, PythonFunction>()
    
    // Draw buffers
    val transientDrawings = mutableStateListOf<DrawCommand>()
    val persistentDrawings = mutableStateListOf<DrawCommand>()
    
    // Output console
    val consoleLogs = mutableStateListOf<String>()
    
    // Error state
    val runtimeError = mutableStateOf<String?>(null)

    // Audios played during execution
    val soundTriggers = mutableStateListOf<String>()

    fun reset() {
        globals.clear()
        functions.clear()
        transientDrawings.clear()
        persistentDrawings.clear()
        consoleLogs.clear()
        runtimeError.value = null
        soundTriggers.clear()
    }

    /**
     * Parse the python code into top-level variables, functions, and initial values
     */
    fun loadScript(code: String) {
        reset()
        try {
            val lines = code.split("\n")
            var i = 0
            while (i < lines.size) {
                val rawLine = lines[i]
                val line = rawLine.trim()
                
                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) {
                    i++
                    continue
                }

                // Check for function definitions
                if (line.startsWith("def ")) {
                    val defHeader = line.substring(4) // e.g., "on_update():"
                    val colonIdx = defHeader.indexOf(":")
                    if (colonIdx != -1) {
                        val header = defHeader.substring(0, colonIdx).trim() // e.g., "on_update()"
                        val openParen = header.indexOf("(")
                        val closeParen = header.indexOf(")")
                        if (openParen != -1 && closeParen != -1) {
                            val funcName = header.substring(0, openParen).trim()
                            val paramString = header.substring(openParen + 1, closeParen)
                            val params = if (paramString.trim().isEmpty()) {
                                emptyList()
                            } else {
                                paramString.split(",").map { it.trim() }
                            }
                            
                            // Collect indented lines
                            val bodyLines = mutableListOf<String>()
                            i++
                            while (i < lines.size) {
                                val nextRawLine = lines[i]
                                if (nextRawLine.trim().isEmpty()) {
                                    i++
                                    continue
                                }
                                
                                val indent = countLeadingSpaces(nextRawLine)
                                if (indent > 0) {
                                    bodyLines.add(nextRawLine)
                                    i++
                                } else {
                                    i-- // Step back to let next loop parse it
                                    break
                                }
                            }
                            functions[funcName] = PythonFunction(funcName, params, bodyLines)
                        }
                    }
                } else {
                    // It is a top-level execution line (typically initializing variables)
                    executeSingleLine(line, isGlobalScope = true)
                }
                i++
            }
        } catch (e: Exception) {
            runtimeError.value = "Lỗi biên dịch: ${e.message}"
            logConsole("CRASH: Lỗi nạp mã nguồn. ${e.message}")
        }
    }

    private fun countLeadingSpaces(s: String): Int {
        var count = 0
        for (char in s) {
            if (char == ' ') count++
            else if (char == '\t') count += 4
            else break
        }
        return count
    }

    fun callFunction(name: String, args: List<Any?> = emptyList()) {
        val func = functions[name] ?: return
        try {
            // Create a local variables context
            val localContext = mutableMapOf<String, Any?>()
            for (index in func.parameters.indices) {
                if (index < args.size) {
                    localContext[func.parameters[index]] = args[index]
                }
            }
            executeBlock(func.bodyLines, localContext)
        } catch (e: Exception) {
            runtimeError.value = "Lỗi runtime trong $name(): ${e.message}"
            logConsole("CRASH trong $name(): ${e.message}")
        }
    }

    private fun executeBlock(lines: List<String>, localContext: MutableMap<String, Any?>) {
        if (lines.isEmpty()) return
        
        // Find the base indentation of this block
        val baseIndent = countLeadingSpaces(lines[0])
        var i = 0
        
        while (i < lines.size) {
            val rawLine = lines[i]
            val line = rawLine.trim()
            
            if (line.isEmpty() || line.startsWith("#")) {
                i++
                continue
            }
            
            // Check if indent is correct
            val indent = countLeadingSpaces(rawLine)
            if (indent < baseIndent) {
                i++
                continue
            }

            // Handle standard controls like 'if', 'else', 'elif'
            if (line.startsWith("if ")) {
                val colonIdx = line.indexOf(":")
                if (colonIdx != -1) {
                    val conditionStr = line.substring(3, colonIdx).trim()
                    val conditionMet = evaluateCondition(conditionStr, localContext)
                    
                    // Collect if block lines
                    val ifBlock = mutableListOf<String>()
                    var j = i + 1
                    while (j < lines.size) {
                        val nextLine = lines[j]
                        if (nextLine.trim().isEmpty()) { j++; continue }
                        if (countLeadingSpaces(nextLine) > indent) {
                            ifBlock.add(nextLine)
                            j++
                        } else {
                            break
                        }
                    }
                    
                    // Collect matching elif / else blocks
                    val elifBlocks = mutableListOf<Pair<String, List<String>>>()
                    var elseBlock: List<String>? = null
                    
                    while (j < lines.size) {
                        val checkLine = lines[j].trim()
                        if (checkLine.isEmpty()) { j++; continue }
                        val checkIndent = countLeadingSpaces(lines[j])
                        
                        if (checkIndent == indent) {
                            if (checkLine.startsWith("elif ")) {
                                val cIdx = checkLine.indexOf(":")
                                val cond = checkLine.substring(5, cIdx).trim()
                                val condBlock = mutableListOf<String>()
                                j++
                                while (j < lines.size) {
                                    val nextL = lines[j]
                                    if (nextL.trim().isEmpty()) { j++; continue }
                                    if (countLeadingSpaces(nextL) > indent) {
                                        condBlock.add(nextL)
                                        j++
                                    } else {
                                        break
                                    }
                                }
                                elifBlocks.add(Pair(cond, condBlock))
                            } else if (checkLine.startsWith("else:")) {
                                val elseBlockLines = mutableListOf<String>()
                                j++
                                while (j < lines.size) {
                                    val nextL = lines[j]
                                    if (nextL.trim().isEmpty()) { j++; continue }
                                    if (countLeadingSpaces(nextL) > indent) {
                                        elseBlockLines.add(nextL)
                                        j++
                                    } else {
                                        break
                                    }
                                }
                                elseBlock = elseBlockLines
                            } else {
                                break
                            }
                        } else {
                            break
                        }
                    }
                    
                    // Run the appropriate block
                    if (conditionMet) {
                        executeBlock(ifBlock, localContext)
                    } else {
                        var elifRun = false
                        for (elif in elifBlocks) {
                            if (evaluateCondition(elif.first, localContext)) {
                                executeBlock(elif.second, localContext)
                                elifRun = true
                                break
                            }
                        }
                        if (!elifRun && elseBlock != null) {
                            executeBlock(elseBlock, localContext)
                        }
                    }
                    
                    i = j // Advance pointer past the if-elif-else construct
                    continue
                }
            } else if (line.startsWith("global ")) {
                // Ignore the line since our parser references globals directly
                // when a local variable isn't found.
                i++
                continue
            } else {
                executeSingleLine(line, isGlobalScope = false, localContext)
                i++
            }
        }
    }

    private fun executeSingleLine(line: String, isGlobalScope: Boolean, localContext: MutableMap<String, Any?> = mutableMapOf()) {
        if (line.isEmpty() || line.startsWith("#")) return

        // 1. Check for variable assignment
        val eqIndex = findAssignmentEquals(line)
        if (eqIndex != -1) {
            val varName = line.substring(0, eqIndex).trim()
            val expression = line.substring(eqIndex + 1).trim()
            
            val value = evaluateExpression(expression, localContext)
            
            // Assign to local context or global context
            if (isGlobalScope || globals.containsKey(varName)) {
                globals[varName] = value
            } else {
                // If designated as global or we want standard python scope behavior,
                // check if the variable exists in globals first. If so, modify global.
                // Otherwise assign local.
                localContext[varName] = value
            }
            return
        }

        // 2. Check for function call
        if (line.endsWith(")") && line.contains("(")) {
            val openP = line.indexOf("(")
            val funcName = line.substring(0, openP).trim()
            val argString = line.substring(openP + 1, line.length - 1)
            val args = if (argString.trim().isEmpty()) {
                emptyList()
            } else {
                splitArguments(argString).map { evaluateExpression(it, localContext) }
            }
            
            executeSystemFunction(funcName, args)
        }
    }

    private fun findAssignmentEquals(line: String): Int {
        var inQuote = false
        var quoteChar = ' '
        for (i in line.indices) {
            val char = line[i]
            if ((char == '\'' || char == '"') && (i == 0 || line[i - 1] != '\\')) {
                if (!inQuote) {
                    inQuote = true
                    quoteChar = char
                } else if (char == quoteChar) {
                    inQuote = false
                }
            }
            
            if (!inQuote && char == '=') {
                // Check if it is a comparison operator: ==, !=, >=, <=
                if (i > 0 && (line[i - 1] == '=' || line[i - 1] == '!' || line[i - 1] == '>' || line[i - 1] == '<')) {
                    continue
                }
                if (i < line.length - 1 && line[i + 1] == '=') {
                    continue
                }
                return i
            }
        }
        return -1
    }

    private fun splitArguments(argsStr: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var parenDepth = 0
        var inQuote = false
        var quoteChar = ' '
        
        for (i in argsStr.indices) {
            val char = argsStr[i]
            if ((char == '\'' || char == '"') && (i == 0 || argsStr[i - 1] != '\\')) {
                if (!inQuote) {
                    inQuote = true
                    quoteChar = char
                } else if (char == quoteChar) {
                    inQuote = false
                }
            }
            
            if (!inQuote) {
                if (char == '(') parenDepth++
                else if (char == ')') parenDepth--
                
                if (char == ',' && parenDepth == 0) {
                    result.add(current.toString())
                    current = StringBuilder()
                    continue
                }
            }
            current.append(char)
        }
        if (current.isNotEmpty()) {
            result.add(current.toString())
        }
        return result.map { it.trim() }
    }

    private fun executeSystemFunction(name: String, args: List<Any?>) {
        when (name) {
            "print" -> {
                val output = args.joinToString(" ") { it.toString() }
                logConsole(output)
            }
            "draw_rect" -> {
                val x = coerceToFloat(args.getOrNull(0) ?: 0)
                val y = coerceToFloat(args.getOrNull(1) ?: 0)
                val w = coerceToFloat(args.getOrNull(2) ?: 0)
                val h = coerceToFloat(args.getOrNull(3) ?: 0)
                val color = args.getOrNull(4)?.toString() ?: "WHITE"
                transientDrawings.add(DrawCommand.Rect(x, y, w, h, color))
            }
            "draw_circle" -> {
                val x = coerceToFloat(args.getOrNull(0) ?: 0)
                val y = coerceToFloat(args.getOrNull(1) ?: 0)
                val r = coerceToFloat(args.getOrNull(2) ?: 0)
                val color = args.getOrNull(3)?.toString() ?: "WHITE"
                transientDrawings.add(DrawCommand.Circle(x, y, r, color))
            }
            "draw_persistent_circle" -> {
                val x = coerceToFloat(args.getOrNull(0) ?: 0)
                val y = coerceToFloat(args.getOrNull(1) ?: 0)
                val r = coerceToFloat(args.getOrNull(2) ?: 0)
                val color = args.getOrNull(3)?.toString() ?: "WHITE"
                persistentDrawings.add(DrawCommand.Circle(x, y, r, color))
            }
            "draw_text" -> {
                val text = args.getOrNull(0)?.toString() ?: ""
                val x = coerceToFloat(args.getOrNull(1) ?: 0)
                val y = coerceToFloat(args.getOrNull(2) ?: 0)
                val size = coerceToFloat(args.getOrNull(3) ?: 16)
                val color = args.getOrNull(4)?.toString() ?: "WHITE"
                transientDrawings.add(DrawCommand.Text(text, x, y, size, color))
            }
            "play_sound" -> {
                val soundName = args.getOrNull(0)?.toString() ?: ""
                soundTriggers.add(soundName)
            }
        }
    }

    private fun logConsole(message: String) {
        consoleLogs.add(message)
        if (consoleLogs.size > 200) {
            consoleLogs.removeAt(0)
        }
    }

    // Coerce Any numeric type safely to Float
    private fun coerceToFloat(value: Any?): Float {
        if (value == null) return 0.0f
        return when (value) {
            is Number -> value.toFloat()
            is String -> value.toFloatOrNull() ?: 0.0f
            is Boolean -> if (value) 1.0f else 0.0f
            else -> 0.0f
        }
    }

    // Condition evaluation (if statements)
    private fun evaluateCondition(conditionStr: String, context: Map<String, Any?>): Boolean {
        try {
            // Check for standard logic operators or comparisons
            val comparisons = listOf(">=", "<=", "==", "!=", ">", "<", " and ", " or ")
            var matchOp = ""
            for (op in comparisons) {
                if (conditionStr.contains(op)) {
                    matchOp = op
                    break
                }
            }

            if (matchOp.isNotEmpty()) {
                if (matchOp == " and " || matchOp == " or ") {
                    val parts = conditionStr.split(matchOp)
                    val left = evaluateCondition(parts[0].trim(), context)
                    val right = evaluateCondition(parts[1].trim(), context)
                    return if (matchOp == " and ") left && right else left || right
                }

                val parts = conditionStr.split(matchOp)
                val leftVal = evaluateExpression(parts[0].trim(), context)
                val rightVal = evaluateExpression(parts[1].trim(), context)

                return compareValues(leftVal, rightVal, matchOp.trim())
            }

            // Check if it is a boolean variable or expression
            val evaluated = evaluateExpression(conditionStr, context)
            if (evaluated is Boolean) return evaluated
            if (evaluated is Number) return evaluated.toDouble() != 0.0
            return evaluated != null
        } catch (e: Exception) {
            return false
        }
    }

    private fun compareValues(left: Any?, right: Any?, op: String): Boolean {
        if (left == null || right == null) return false
        
        if (left is Number && right is Number) {
            val l = left.toDouble()
            val r = right.toDouble()
            return when (op) {
                ">=" -> l >= r
                "<=" -> l <= r
                "==" -> l == r
                "!=" -> l != r
                ">" -> l > r
                "<" -> l < r
                else -> false
            }
        }
        
        // String comparisons
        val lStr = left.toString()
        val rStr = right.toString()
        return when (op) {
            "==" -> lStr == rStr
            "!=" -> lStr != rStr
            else -> false
        }
    }

    // Evaluate basic math and expressions (e.g. x + 5, random_range(a,b), etc.)
    private fun evaluateExpression(exprStr: String, context: Map<String, Any?>): Any? {
        val trimmed = exprStr.trim()
        
        // 1. Check for strings
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || 
            (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length - 1)
        }

        // 2. Check for numeric literals
        val intValue = trimmed.toIntOrNull()
        if (intValue != null) return intValue
        
        val doubleValue = trimmed.toDoubleOrNull()
        if (doubleValue != null) return doubleValue

        if (trimmed == "True") return true
        if (trimmed == "False") return false
        if (trimmed == "None") return null

        // 3. Check for specific functional calls
        if (trimmed.startsWith("random_range(") && trimmed.endsWith(")")) {
            val argString = trimmed.substring(13, trimmed.length - 1)
            val args = splitArguments(argString).map { evaluateExpression(it, context) }
            val min = coerceToFloat(args.getOrNull(0) ?: 0)
            val max = coerceToFloat(args.getOrNull(1) ?: 100)
            return min + Random.nextFloat() * (max - min)
        }

        if (trimmed.startsWith("str(") && trimmed.endsWith(")")) {
            val inner = trimmed.substring(4, trimmed.length - 1)
            return evaluateExpression(inner, context)?.toString() ?: "None"
        }

        if (trimmed.startsWith("abs(") && trimmed.endsWith(")")) {
            val inner = trimmed.substring(4, trimmed.length - 1)
            val v = coerceToFloat(evaluateExpression(inner, context) ?: 0)
            return kotlin.math.abs(v)
        }

        // 4. Check for operators: +, -, *, /
        val ops = listOf("+", "-", "*", "/")
        for (op in ops) {
            val opIdx = findOperatorIndex(trimmed, op)
            if (opIdx != -1) {
                val leftExpr = trimmed.substring(0, opIdx).trim()
                val rightExpr = trimmed.substring(opIdx + 1).trim()
                
                val leftVal = evaluateExpression(leftExpr, context)
                val rightVal = evaluateExpression(rightExpr, context)
                
                if (op == "+" && (leftVal is String || rightVal is String)) {
                    return leftVal.toString() + rightVal.toString()
                }

                if (leftVal is Number && rightVal is Number) {
                    val l = leftVal.toDouble()
                    val r = rightVal.toDouble()
                    return when (op) {
                        "+" -> l + r
                        "-" -> l - r
                        "*" -> l * r
                        "/" -> if (r != 0.0) l / r else 0.0
                        else -> 0.0
                    }
                }
            }
        }

        // 5. Check if it's a variable in current context, or globals
        if (context.containsKey(trimmed)) {
            return context[trimmed]
        }
        if (globals.containsKey(trimmed)) {
            return globals[trimmed]
        }

        // Fallback to string representation if unrecognized
        return trimmed
    }

    private fun findOperatorIndex(expr: String, op: String): Int {
        var inQuote = false
        var quoteChar = ' '
        var parenDepth = 0
        
        // Search right to left for operators to preserve standard associativity
        for (i in expr.length - 1 downTo 0) {
            val char = expr[i]
            if ((char == '\'' || char == '"') && (i == 0 || expr[i - 1] != '\\')) {
                if (!inQuote) {
                    inQuote = true
                    quoteChar = char
                } else if (char == quoteChar) {
                    inQuote = false
                }
            }
            
            if (!inQuote) {
                if (char == ')') parenDepth++
                else if (char == '(') parenDepth--
                
                if (char.toString() == op && parenDepth == 0) {
                    // Prevent catching signed numbers like negative values e.g. -5
                    if (op == "-" && (i == 0 || expr[i - 1] == '+' || expr[i - 1] == '-' || expr[i - 1] == '*' || expr[i - 1] == '/' || expr[i - 1] == '=')) {
                        continue
                    }
                    return i
                }
            }
        }
        return -1
    }
}
