package org.apache.flink.contrib.tensorflow.streaming.functions

import org.apache.flink.contrib.tensorflow.common.functions.util.FunctionModelOperations
import org.apache.flink.contrib.tensorflow.streaming.functions.util.FunctionModelCheckpointOperations
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.co.RichCoProcessFunction

/**
  * An abstract co-process function with TensorFlow model support.
  */
abstract class AbstractCoProcessFunction[IN1, IN2, OUT] extends RichCoProcessFunction[IN1, IN2, OUT]
  with CheckpointedFunction
  with FunctionModelOperations
  with FunctionModelCheckpointOperations {

}
