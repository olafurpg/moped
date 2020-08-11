package moped.internal.diagnostics

import moped.reporters.Diagnostic

class DiagnosticException(val d: Diagnostic)
    extends Exception(d.message, d.throwable.orNull) {}
