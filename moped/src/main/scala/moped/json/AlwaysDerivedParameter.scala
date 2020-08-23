package moped.json

/**
 * Marker trait for parameters who should always be derived from DecodingContext.
 *
 * By default, Moped uses the default value for parameters unless the
 * DecodingContext.json element contains a matching field name. If the
 * parameter has a type which is a subtype of this trait then the default
 * value is always ignored in favor of the JsonDecoder[A]. The use-case for
 * this trait is to create an Application-like class but with additional
 * fields and methods.
 */
trait AlwaysDerivedParameter
