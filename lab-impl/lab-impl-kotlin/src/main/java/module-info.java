module lab.impl.kotlin {
	requires lab.impl.api;
	requires kotlin.stdlib;
	requires tools.jackson.databind;
	requires tools.jackson.dataformat.xml;
	requires com.fasterxml.jackson.annotation;

	opens com.redwood.collections.kotlin to tools.jackson.databind;
	exports com.redwood.collections.kotlin;
}
