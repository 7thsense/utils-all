.PHONY: default
default:
	sbt +publishLocal
	sbt spark/publishLocal utils-collections-spark/publishLocal

.PHONY: publish
publish:
	sbt +publish
	sbt spark/publish utils-collections-spark/publish
