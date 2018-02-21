package com.theseventhsense.utils.akka

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

/**
  * Akka Streams graph stage that accepts a *SORTED* list stream of item A and
  * fuses them into an output B records. Equivalent of groupBy but relies on the sorted nature of the stream
  * to allow materialization prior to the end of the stream.
  */
class SortedGroupByGraphStage[A](compare: (A, A) => Boolean)
  extends GraphStage[FlowShape[A, Seq[A]]] {
  val in: Inlet[A] = Inlet[A]("GroupBy.in")
  val out: Outlet[Seq[A]] = Outlet[Seq[A]]("GroupBy.out")

  val shape: FlowShape[A, Seq[A]] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      var buffer = Seq.empty[A]
      setHandler(
        in,
        new InHandler {
          override def onPush(): Unit = {
            val elem = grab(in)
            if (buffer.nonEmpty) {
              if (compare(buffer.head, elem)) {
                buffer = buffer :+ elem
                pull(in)
              } else {
                push(out, buffer)
                buffer = Seq(elem)
              }
            } else {
              buffer = Seq(elem)
              pull(in)
            }
          }

          override def onUpstreamFinish(): Unit = {
            if (buffer.nonEmpty) emit(out, buffer)
            complete(out)
          }
        }
      )

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          pull(in)
        }
      })
    }
}
