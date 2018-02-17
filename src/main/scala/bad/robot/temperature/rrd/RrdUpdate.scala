package bad.robot.temperature.rrd

import bad.robot.logging.Log
import bad.robot.temperature.{Error, Measurement, RrdError, SensorReading, Temperature, _}
import org.rrd4j.core.RrdDb

import scalaz.\/
import scalaz.\/.fromTryCatchNonFatal

case class RrdUpdate(monitored: List[Host], measurement: Measurement) {

  private val UnknownValues = List().padTo(monitored.size * RrdFile.MaxSensors, SensorReading("Unknown", Temperature(Double.NaN)))

  def apply(): Error \/ Unit = {
    fromTryCatchNonFatal {
      val database = new RrdDb(RrdFile.file)
      val sample = database.createSample()
      val indexOf = monitored.indexOf(measurement.host)
      Log.debug(s"Index of ${measurement.host.name } is $indexOf (-1 means it couldn't be found)")
      val temperatures = UnknownValues.patch(indexOf * RrdFile.MaxSensors, measurement.temperatures, measurement.temperatures.size)
      sample.setValues(database, measurement.time, temperatures.map(_.temperature.celsius): _*)
      Log.debug(s"rrd -> ${measurement.host.name} ${measurement.time} ${temperatures.map(t => t.name + " " + t.temperature.celsius).mkString(", ")}")
      database.close()
    }.leftMap(error => {
      RrdError(messageOrStackTrace(error))
    })
  }

  def messageOrStackTrace(error: Throwable): String = {
    Option(error.getMessage).getOrElse(stackTraceOf(error)) 
  }
}
