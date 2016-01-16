package bad.robot.temperature.ds18b20

import java.io.File

import bad.robot.temperature.{FailedToFindFile, FileError, UnexpectedError}
import org.specs2.matcher.DisjunctionMatchers._
import org.specs2.mutable.Specification


class SensorReaderTest extends Specification {

  "Valid sensor file" >> {
    val file = List(new File("src/test/resources/examples/28-000005e2fdc2/w1_slave"))
    SensorReader(file).read must be_\/-
  }


  "No sensor files found should be an error" >> {
    val files = List()
    SensorReader(files).read must be_-\/.like {
      case e: FailedToFindFile => ok
    }
  }

  "File doesn't exist" >> {
    val files = List(
      new File("src/test/resources/examples/28-000005e2fdc2/w1_slave"),
      new File("uh oh")
    )
    SensorReader(files).read must be_-\/.like {
      case e: FileError => ok
    }
  }

  "Empty file" >> {
    val file = List(new File("src/test/resources/examples/empty-file.txt"))
    SensorReader(file).read must be_-\/.like {
      case e: UnexpectedError => e.message must contain("Problem reading file, is it empty?")
    }
  }

  "Nonsense file (contents would fail to parse)" >> {
    val file = List(new File("src/test/resources/examples/nonsense-file.txt"))
    SensorReader(file).read must be_-\/.like {
      case e: UnexpectedError => e.message must contain("Failed to recognise sensor data")
    }
  }

}
