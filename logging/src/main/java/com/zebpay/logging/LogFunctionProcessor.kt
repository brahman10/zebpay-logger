package com.zebpay.logging

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.OutputStreamWriter

class LogFunctionProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("com.zebpay.logging.LogFunction")
            .filterIsInstance<KSClassDeclaration>()

        symbols.forEach { classDecl ->
            val className = classDecl.simpleName.asString()
            val packageName = classDecl.packageName.asString()

            val file = codeGenerator.createNewFile(
                Dependencies(false, classDecl.containingFile!!),
                packageName,
                "${className}_LogWrapper"
            )

            val writer = OutputStreamWriter(file, Charsets.UTF_8)
            writer.write("package $packageName\n\n")
            writer.write("import android.util.Log\n\n")

            classDecl.getAllFunctions()
                .filter { func -> func.isPublicFunction() }
                .forEach { func ->
                    val funName = func.simpleName.asString()
                    val params = func.parameters.joinToString(", ") { param ->
                        val name = param.name?.asString() ?: "param"
                        val type = param.type.resolve().declaration.qualifiedName?.asString() ?: "Any"
                        "$name: $type"
                    }

                    val callParams = func.parameters.joinToString(", ") {
                        it.name?.asString() ?: "param"
                    }

                    val returnType = func.returnType?.resolve()?.declaration?.qualifiedName?.asString() ?: "Unit"
                    val receiver = classDecl.simpleName.asString()

                    // Generate method with logging and delegation
                    writer.write("fun $receiver.$funName(${params})")

                    if (returnType != "kotlin.Unit") {
                        writer.write(": $returnType")
                    }

                    writer.write(" {\n")
                    writer.write("    Log.d(\"LogFunction\", \"$funName called\")\n")
                    val call = "this.$funName($callParams)"
                    if (returnType != "kotlin.Unit") {
                        writer.write("    return $call\n")
                    } else {
                        writer.write("    $call\n")
                    }
                    writer.write("}\n\n")
                }

            writer.close()
        }

        return emptyList()
    }

    private fun KSFunctionDeclaration.isPublicFunction(): Boolean {
        return this.origin == Origin.KOTLIN && this.simpleName.asString() != "<init>"
    }
}
