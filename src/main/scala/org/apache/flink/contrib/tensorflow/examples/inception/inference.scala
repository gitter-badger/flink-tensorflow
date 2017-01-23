package org.apache.flink.contrib.tensorflow.examples.inception

import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.{List => JavaList}

import com.twitter.bijection.Conversion._
import org.apache.flink.contrib.tensorflow.examples.inception.InceptionModel._
import org.apache.flink.contrib.tensorflow.models.Model.RunContext
import org.apache.flink.contrib.tensorflow.models.Signature
import org.apache.flink.contrib.tensorflow.models.generic.{DefaultGraphLoader, GenericModel, GraphLoader}
import org.apache.flink.contrib.tensorflow.types.TensorInjections._
import org.apache.flink.contrib.tensorflow.types.TensorValue
import org.apache.flink.contrib.tensorflow.util.GraphUtils
import org.apache.flink.core.fs.Path
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

/**
  * Infers labels for images.
  *
  * @param modelPath the directory containing the model files.
  */
@SerialVersionUID(1L)
class InceptionModel(modelPath: URI) extends GenericModel[InceptionModel] {

  protected val LOG: Logger = LoggerFactory.getLogger(classOf[InceptionModel])

  override protected def graphLoader: GraphLoader =
    new DefaultGraphLoader(new Path(new Path(modelPath), "tensorflow_inception_graph.pb"))

  @transient lazy val labels: List[String] = GraphUtils.readAllLines(
    new Path(new Path(modelPath), "imagenet_comp_graph_label_strings.txt"), StandardCharsets.UTF_8).asScala.toList

  def labeled2(tensor: LabelTensor): Array[LabeledImage] = {
    Array(LabeledImage(List[(Float,String)]().asJava))
  }

  /**
    * Convert the label tensor to a list of labels.
    */
  def labeled(tensor: LabelTensor): Array[LabeledImage] = {
    // the tensor consists of a row per image, with columns representing label probabilities
    val t = tensor.toTensor
    try {
      require(t.numDimensions() == 2, "expected a [M N] shaped tensor")
      val matrix = Array.ofDim[Float](t.shape()(0).toInt,t.shape()(1).toInt)
      t.copyTo(matrix)
      matrix.map { row =>
        LabeledImage(row.toList.zip(labels).sortWith(_._1 > _._1).take(5).asJava)
      }
    }
    finally {
      t.close()
    }
  }

  /**
    * Label an image according to the inception model.
    */
  val label: InferenceSignature[InceptionModel] = new InferenceSignature()
}

@SerialVersionUID(1L)
class InferenceSignature[M]
  extends Signature[M,ImageTensor,LabelTensor] {

  override def run(model: M, context: RunContext, input: ImageTensor): LabelTensor = {
    val i = input.toTensor
    try {
      val cmd = context.session.runner().feed("input", i).fetch("output")
      val o = cmd.run()
      try {
        o.get(0).as[TensorValue]
      }
      finally {
        o.asScala.foreach(_.close())
      }
    }
    finally {
      i.close()
    }
  }
}

object InceptionModel {

  /**
    * A set of images encoded as a 4D tensor of floats.
    */
  type ImageTensor = TensorValue

  /**
    * A set of labels encoded a 2D tensor of floats.
    */
  type LabelTensor = TensorValue

  /**
    * An image with associated labels (sorted by probability descending)
    */
  case class LabeledImage(labels: JavaList[(Float,String)])
}
