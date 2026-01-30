import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._

import scala.io.Source
import scala.util.Using

final case class Registro(
                           ID: Int,
                           Tipo_Ataque: String,
                           IP_Origen: String,
                           IP_Destino: String,
                           Severidad: String,
                           Pais_Origen: String,
                           Pais_Destino: String,
                           Fecha_Ataque: String,
                           Duracion_Minutos: Int,
                           Contenido_Sensible_Comprometido: Boolean
                         )

object ExamenParcial extends IOApp.Simple {

  private val xa: Transactor[IO] =
    Transactor.fromDriverManager[IO](
      driver = "com.mysql.cj.jdbc.Driver",
      url = "jdbc:mysql://localhost:3306/ataques",
      user = "root",
      password = "Malu_2005",
      logHandler = None
    )

  private def insertarRegistro(r: Registro): ConnectionIO[Int] =
    sql"""
      INSERT INTO Registro (
        ID, Tipo_Ataque, IP_Origen, IP_Destino, Severidad,
        Pais_Origen, Pais_Destino, Fecha_Ataque, Duracion_Minutos,
        Contenido_Sensible_Comprometido
      )
      VALUES (
        ${r.ID}, ${r.Tipo_Ataque}, ${r.IP_Origen}, ${r.IP_Destino}, ${r.Severidad},
        ${r.Pais_Origen}, ${r.Pais_Destino}, ${r.Fecha_Ataque}, ${r.Duracion_Minutos},
        ${r.Contenido_Sensible_Comprometido}
      )
    """.update.run

  private def listarTodo(): ConnectionIO[List[Registro]] =
    sql"""
      SELECT
        ID, Tipo_Ataque, IP_Origen, IP_Destino, Severidad,
        Pais_Origen, Pais_Destino, Fecha_Ataque, Duracion_Minutos,
        Contenido_Sensible_Comprometido
      FROM Registro
    """.query[Registro].to[List]

  private def limpiarTabla(): ConnectionIO[Int] =
    sql"TRUNCATE TABLE Registro".update.run

  private def leerCsv(path: String): IO[List[String]] =
    IO.blocking {
      Using.resource(Source.fromFile(path)) { src =>
        src.getLines().drop(1).toList
      }
    }


  private def parseLinea(linea: String): Registro = {
    val c = linea.split(",").map(_.trim)

    Registro(
      ID = c(0).toInt,
      Tipo_Ataque = c(1),
      IP_Origen = c(2),
      IP_Destino = c(3),
      Severidad = c(4),
      Pais_Origen = c(5),
      Pais_Destino = c(6),
      Fecha_Ataque = c(7),
      Duracion_Minutos = c(8).toInt,
      Contenido_Sensible_Comprometido = c(9).toBoolean
    )
  }

  def run: IO[Unit] = {
    val csvPath = raw"C:\Users\Diego\Documents\programacion funciona\ClasesPF\src\main\resources\data\ataques.csv"

    val programa =
      for {
        lineas <- leerCsv(csvPath)
        registros = lineas.map(parseLinea)

        resultados <- (for {
          _ <- limpiarTabla()
          _ <- registros.traverse_(insertarRegistro)
          xs <- listarTodo()
        } yield xs).transact(xa)

        _ <- IO.println("\n--- REGISTRO DE ATAQUES ---")
        _ <- resultados.traverse_(r =>
          IO.println(
            s"${r.ID} | ${r.Tipo_Ataque} | ${r.IP_Origen} -> ${r.IP_Destino} |" +
              s" ${r.Severidad} | ${r.Pais_Origen} -> ${r.Pais_Destino} | " +
              s"${r.Fecha_Ataque} | ${r.Duracion_Minutos} | ${r.Contenido_Sensible_Comprometido}"
          )
        )
        _ <- IO.println(s"\nTotal de casos: ${resultados.size}")
      } yield ()

    programa
  }
}

