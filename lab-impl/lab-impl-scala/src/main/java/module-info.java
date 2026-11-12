module lab.impl.scala {
	requires lab.impl.api;
	requires tools.jackson.databind;
	requires tools.jackson.dataformat.xml;
	requires com.fasterxml.jackson.annotation;
	requires org.scala.lang.scala3.library;

	opens com.redwood.collections.scala to tools.jackson.databind;
	exports com.redwood.collections.scala;
}
