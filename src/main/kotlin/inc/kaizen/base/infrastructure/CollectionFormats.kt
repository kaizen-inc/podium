package inc.kaizen.base.infrastructure

/**
 * Utility classes for serializing multi-value query/form parameters into
 * the formats defined by the OpenAPI Specification.
 */
object CollectionFormats {

    /** Comma-separated values: `a,b,c` */
    open class CSVParams(val params: List<String> = emptyList()) {
        constructor(vararg params: String) : this(params.toList())
        override fun toString(): String = params.joinToString(",")
    }

    /** Space-separated values: `a b c` */
    open class SSVParams(params: List<String> = emptyList()) : CSVParams(params) {
        constructor(vararg params: String) : this(params.toList())
        override fun toString(): String = params.joinToString(" ")
    }

    /** Tab-separated values: `a\tb\tc` */
    class TSVParams(params: List<String> = emptyList()) : CSVParams(params) {
        constructor(vararg params: String) : this(params.toList())
        override fun toString(): String = params.joinToString("\t")
    }

    /** Pipe-separated values: `a|b|c` */
    class PIPESParams(params: List<String> = emptyList()) : CSVParams(params) {
        constructor(vararg params: String) : this(params.toList())
        override fun toString(): String = params.joinToString("|")
    }

    /** Alias for [SSVParams]. */
    class SPACEParams(params: List<String> = emptyList()) : SSVParams(params) {
        constructor(vararg params: String) : this(params.toList())
    }
}