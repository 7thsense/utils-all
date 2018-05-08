.PHONY: default
default:
	sbt +publishLocal
	sbt utils-spark/publishLocal utils-collections-spark/publishLocal

.PHONY: publish
publish:
	sbt +publish
	sbt utils-spark/publish utils-collections-spark/publish
