module lab.impl.java {
	requires lab.impl.api;
	requires tools.jackson.databind;
	requires tools.jackson.core;
	requires tools.jackson.dataformat.xml;
	requires com.fasterxml.jackson.annotation;
	requires org.jetbrains.annotations;

	opens com.redwood.collections.java to tools.jackson.databind;
	exports com.redwood.collections.java;
}
