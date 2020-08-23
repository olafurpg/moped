package moped.json

/**
 * Marker trait for parameters that should always be marked as @Hidden().
 *
 * By default, parameters are non-hidden unless the user explicitly adds the
 * @Hidden() annotation. When parameter have types that are a subtype of this
 * trait then the parameter is treated as hidden even if there is no @Hidden()
 * annotation.
 */
trait AlwaysHiddenParameter
