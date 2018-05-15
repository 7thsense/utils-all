.PHONY: default
default:
	sbt +publishLocal
	sbt utils-spark/publishLocal 

.PHONY: publish
publish:
	sbt +publish
	sbt utils-spark/publish 
