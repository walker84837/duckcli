package org.winlogon

import org.jline.reader.{Completer, LineReader, ParsedLine, Candidate}

abstract class ModelCompleter(models: Map[String, String]) extends Completer {
  override def complete(reader: LineReader, line: ParsedLine, candidates: java.util.List[Candidate]): Unit = {
    val buffer = line.line()
    val suggestions = models.keys.filter(_.startsWith(buffer)).toList
    suggestions.foreach { suggestion =>
      candidates.add(new Candidate(suggestion))
    }
  }
}
