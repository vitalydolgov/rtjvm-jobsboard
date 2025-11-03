package com.example.jobsboard.components

import tyrian.*
import tyrian.Html.*
import tyrian.http.*
import tyrian.cmds.Logger
import cats.effect.IO
import io.circe.generic.auto.*

import com.example.jobsboard.*
import com.example.jobsboard.common.*
import com.example.jobsboard.domain.job.*
import org.scalajs.dom.HTMLInputElement

object FilterPanel {
  trait Message extends App.Message
  case object GetFilters extends Message
  case class FilterPanelError(message: String) extends Message
  case class FilterPanelSuccess(possibleFilters: JobFilter) extends Message
  case class UpdateSalary(salary: Int) extends Message
  case class UpdateRemote(remote: Boolean) extends Message
  case class UpdateCheckbox(groupName: String, value: String, isChecked: Boolean) extends Message
  case object ApplyFilters extends Message

  object Endpoints {
    val getFilters = new Endpoint[Message] {
      override val location: String = Constants.endpoints.filters
      override val method: Method = Method.Get

      override val onResponse: Response => Message =
        Endpoint.onResponse[JobFilter, Message](FilterPanelSuccess(_), FilterPanelError(_))

      override val onError: HttpError => Message = err => FilterPanelError(err.toString)
    }
  }

  object Commands {
    def getFilters(): Cmd[IO, Message] =
      Endpoints.getFilters.call()
  }
}

final case class FilterPanel(
    applyFilters: Map[String, Set[String]] => App.Message = _ => App.NoOp,
    possibleFilters: JobFilter = JobFilter(),
    selectedFilters: Map[String, Set[String]] = Map(),
    maxSalary: Int = 0,
    remote: Boolean = false,
    errorOpt: Option[String] = None,
    isDirty: Boolean = false
) extends Component[App.Message, FilterPanel] {

  import FilterPanel.*

  override def initCommand: Cmd[IO, App.Message] = Cmd.Emit(GetFilters)

  override def update(message: App.Message): (FilterPanel, Cmd[IO, App.Message]) = message match {
    case GetFilters => (this, Commands.getFilters())
    case FilterPanelSuccess(filters) =>
      (this.copy(possibleFilters = filters, errorOpt = None), Cmd.None)
    case FilterPanelError(message) => (this.copy(errorOpt = Some(message)), Cmd.None)
    case UpdateSalary(value)       => (this.copy(maxSalary = value, isDirty = true), Cmd.None)
    case UpdateRemote(value)       => (this.copy(remote = value, isDirty = true), Cmd.None)
    case UpdateCheckbox(groupName, value, isChecked) =>
      val oldGroup = selectedFilters.get(groupName).getOrElse(Set())
      val newGroup = if (isChecked) oldGroup + value else oldGroup - value
      val newSelectedFilters = selectedFilters + (groupName -> newGroup)
      (
        this.copy(selectedFilters = newSelectedFilters, isDirty = true),
        Logger.consoleLog[IO](s"filters = $newSelectedFilters")
      )
    case ApplyFilters => (this.copy(isDirty = false), Cmd.Emit(applyFilters(selectedFilters)))
    case _            => (this, Cmd.None)
  }

  private def errorTextOpt =
    errorOpt
      .map { err =>
        div(`class` := "page-status-error")(err)
      }
      .getOrElse(div())

  private def filterGroup(groupName: String, contents: Html[App.Message]) =
    div(`class` := "accordion-item")(
      h2(`class` := "accordion-header", id := s"heading$groupName")(
        button(
          `class` := "accordion-button collapsed",
          `type` := "button",
          attribute("data-bs-toggle", "collapse"),
          attribute("data-bs-target", s"#collapse$groupName"),
          attribute("aria-expanded", "false"),
          attribute("aria-controls", s"collapse$groupName")
        )(
          groupName
        )
      ),
      div(
        `class` := "accordion-collapse collapse",
        id := s"collapse$groupName",
        attribute("aria-labelledby", "headingOne"),
        attribute("data-bs-parent", "#accordionExample")
      )(
        div(`class` := "accordion-body")(
          contents
        )
      )
    )

  private def salaryFilter =
    filterGroup(
      "Salary",
      div(`class` := "mb-3")(
        label(`for` := "filter-salary")("Min (in local currency)"),
        input(
          `type` := "number",
          id := "filter-salary",
          onInput(str => UpdateSalary(if (str.isEmpty) 0 else str.toInt))
        )
      )
    )

  private def remoteCheckbox =
    filterGroup(
      "Remote",
      div(`class` := "form-check")(
        label(
          `class` := "form-check-label",
          `for` := s"filter-remote"
        )("Remote"),
        input(
          `class` := "form-check-input",
          `type` := "checkbox",
          id := s"filter-remote",
          checked(remote),
          onEvent(
            "change",
            event => {
              val checkbox = event.target.asInstanceOf[HTMLInputElement]
              UpdateRemote(checkbox.checked)
            }
          )
        )
      )
    )

  private def checkbox(groupName: String, value: String, selectedValues: Set[String]) =
    div(`class` := "form-check")(
      label(
        `class` := "form-check-label",
        `for` := s"filter-$groupName-$value"
      )(value),
      input(
        `class` := "form-check-input",
        `type` := "checkbox",
        id := s"filter-$groupName-$value",
        checked(selectedValues.contains(value)),
        onEvent(
          "change",
          event => {
            val checkbox = event.target.asInstanceOf[HTMLInputElement]
            UpdateCheckbox(groupName, value, checkbox.checked)
          }
        )
      )
    )

  private def checkboxGroup(groupName: String, values: List[String]) = {
    val selectedValues = selectedFilters.get(groupName).getOrElse(Set())
    filterGroup(
      groupName,
      div(`class` := "mb-3")(
        values.map(value => checkbox(groupName, value, selectedValues))
      )
    )
  }

  private def applyFiltersButton =
    div(`class` := "jvm-accordion-search-btn")(
      button(
        `class` := "btn btn-primary",
        `type` := "button",
        disabled(!isDirty),
        onClick(ApplyFilters)
      )("Apply Filters")
    )

  override def view: Html[App.Message] =
    div(`class` := "accordion accordion-flush", id := "accordionFlushExample")(
      div(`class` := "accordion-item")(
        h2(`class` := "accordion-header", id := "flush-headingOne")(
          button(
            `class` := "accordion-button",
            id := "accordion-search-filter",
            `type` := "button",
            attribute("data-bs-toggle", "collapse"),
            attribute("data-bs-target", "#flush-collapseOne"),
            attribute("aria-expanded", "true"),
            attribute("aria-controls", "flush-collapseOne")
          )(
            div(`class` := "jvm-recent-jobs-accordion-body-heading")(
              h3(span("Search"), text(" Filters"))
            )
          )
        ),
        div(
          `class` := "accordion-collapse collapse show",
          id := "flush-collapseOne",
          attribute("aria-labelledby", "flush-headingOne"),
          attribute("data-bs-parent", "#accordionFlushExample")
        )(
          div(`class` := "accordion-body p-0")(
            errorTextOpt,
            salaryFilter,
            remoteCheckbox,
            checkboxGroup("Companies", possibleFilters.companies),
            checkboxGroup("Locations", possibleFilters.locations),
            checkboxGroup("Countries", possibleFilters.countries),
            checkboxGroup("Seniorities", possibleFilters.seniorities),
            checkboxGroup("Tags", possibleFilters.tags),
            applyFiltersButton
          )
        )
      )
    )

}
