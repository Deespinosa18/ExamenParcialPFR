## Formato utilizado con la ia 

mira el profesor nos dejo utilizar ia para el examen ya tengo practicamente armado todo lo que pidio que es generar la tabla en
se paso el codigo inicial 
```Scala
import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._

import scala.util.Using
import scala.io.Source

final case class Estudiante(
  id: Int,
  Tipo_Ataque: String,
  IP_Origen: Int,
  IP_Destino: Int,
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
      url = "jdbc:mysql://localhost:3306/estudiantes_db",
      user = "root",
      password = "Malu_2005",
      logHandler = None
    )

  private def insertcsv(
    id: Int,
    Tipo_Ataque: String,
    IP_Origen: Int,
    IP_Destino: Int,
    Severidad: String,
    Pais_Origen: String,
    Pais_Destino: String,
    Fecha_Ataque: String,
    Duracion_Minutos: Int,
    Contenido_Sensible_Comprometido: Boolean
  ): ConnectionIO[Int] =
    sql"""
      INSERT INTO Registro (
        ID,
        Tipo_Ataque,
        IP_Origen,
        IP_Destino,
        Severidad,
        Pais_Origen,
        Pais_Destino,
        Fecha_Ataque,
        Duracion_Minutos,
        Contenido_Sensible_Comprometido:
      )
      VALUES (
        $id,
        $Tipo_Ataque,
        $IP_Origen,
        $IP_Destino $Severidad,
        $Pais_Origen,
        $Pais_Destino,
        $Fecha_Ataque,
        $Duracion_Minutos,
        $Contenido_Sensible_Comprometido,
      )
    """.update.run

  private def parseLinea(linea: String): Either[String, (String, Int, Int, String)] = {
    val cols = linea.split(",").map(_.trim)

    if (cols.length < 4) Left(s"Fila inválida (faltan columnas): $linea")
    else {
      val nombre = cols(0)

      val edadE =
        Either
          .catchOnly[NumberFormatException](cols(1).toInt)
          .leftMap(_ => s"Edad inválida: ${cols(1)} en $linea")

      val calE =
        Either
          .catchOnly[NumberFormatException](cols(2).toInt)
          .leftMap(_ => s"Calificación inválida: ${cols(2)} en $linea")

      val genero = cols(3)

      (edadE, calE).mapN((edad, cal) => (nombre, edad, cal, genero))
    }
  }

  private def leerCsv(path: String): IO[List[String]] =
    IO.blocking {
      Using.resource(
        Source.fromFile(
          "C:\\Users\\Diego\\Documents\\programacion funciona\\ClasesPF\\src\\main\\resources\\data\\ataques.csv"
        )
      ) { src =>
        src.getLines().drop(1).toList
      }
    }

  def run: IO[Unit] = {
    val csvPath =
      raw"C:\\Users\\Diego\\Documents\\programacion funciona\\ClasesPF\\src\\main\\resources\\data\\ataques.csv"

    val programaDb: IO[List[Estudiante]] =
      for {
        lineas <- leerCsv(csvPath)

        inserts = lineas.flatMap { l =>
          parseLinea(l

```
luego se corrigio ya que estaba dando cosas demas
chat:
estas complicando mucho el programa yo hice algo mas simplw, mira este es el csv una parte de obviamente
Tienes toda la razón: tu CSV es mucho más simple y mi versión anterior se enredó por dos cosas:

Las IP no son Int → vienen como "192.168.1.1" (texto), así que deben ser String.

El CSV ya trae todo “limpio” (10 columnas), entonces el parseo puede ser directo.

Abajo te dejo una versión simple, corta y muy parecida a tu estilo, que:

Lee el CSV

Inserta cada fila en MySQL

(Opcional) hace TRUNCATE

Lista y muestra

```Scala
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
      url = "jdbc:mysql://localhost:3306/estudiantes_db",
      user = "root",
      password = "Malu_2005"
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
        src.getLines().drop(1).toList // salta header
      }
    }

  // Parseo SIMPLE (porque tu CSV ya viene bien formateado)
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
          _ <- limpiarTabla() // si NO quieres truncar, borra esta línea
          _ <- registros.traverse_(insertarRegistro)
          xs <- listarTodo()
        } yield xs).transact(xa)

        _ <- IO.println("\n--- REGISTRO DE ATAQUES ---")
        _ <- resultados.traverse_(r =>
          IO.println(
            s"${r.ID} | ${r.Tipo_Ataque} | ${r.IP_Origen} -> ${r.IP_Destino} | ${r.Severidad} | ${r.Pais_Origen} -> ${r.Pais_Destino} | ${r.Fecha_Ataque} | ${r.Duracion_Minutos} | ${r.Contenido_Sensible_Comprometido}"
          )
        )
        _ <- IO.println(s"\nTotal de casos: ${resultados.size}")
      } yield ()

    programa
  }

```
codigo final
