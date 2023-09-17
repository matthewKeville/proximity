package main

import (
  "proximity-client/http"
  "proximity-client/event"
  "os"
  "fmt"
  "sort"
  "github.com/charmbracelet/bubbletea"
  "github.com/charmbracelet/bubbles/table"
  "github.com/charmbracelet/lipgloss"
)


var baseStyle = lipgloss.NewStyle().
  BorderStyle(lipgloss.NormalBorder()).
  BorderForeground(lipgloss.Color("240"))

type model struct {
  table table.Model
  events []event.Event
  filterEvents []event.Event
  showField map[string]bool
}

func ToTableRows(evs []event.Event,sf map[string]bool) []table.Row {
  rows := []table.Row{}
  for _, evt := range evs {
    rows = append(rows,ToTableRow(evt,sf))
  }
  return rows
}

func ToTableRow(e event.Event,sf map[string]bool) table.Row {

  cl := []string{}

  if (sf["EventType"]) {
    cl = append(cl,e.EventType)
  }

  if (sf["Name"]) {
    cl = append(cl,e.Name)
  }
  if (sf["Location.Name"]) {
    cl = append(cl,e.Location.Name)
  }
  if (sf["Location.Region"]) {
    cl = append(cl,e.Location.Region)
  }
  if (sf["Location.Locality"]) {
    cl = append(cl,e.Location.Locality)
  }
  if (sf["_DistanceMiles"]) { 
    cl = append(cl,fmt.Sprintf("%f",e.Distance))
  }
  if (sf["_DistanceDays"]) {
    cl = append(cl,fmt.Sprintf("%d",e.DaysFromNow))
  }
  if (sf["_Start_Month_Name"]) {
    cl = append(cl,fmt.Sprintf("%s",e.Start.Month()))
  }
  
  return cl
}


func main() {
  
  p := tea.NewProgram(initialModel())

  if _, err := p.Run(); err != nil {
    fmt.Printf("An error occurred, error : %v",err)
    os.Exit(1)
  }

}

func initialModel() model {


  es := http.GetEvents()

  sort.Slice(es, func(i, j int) bool {
    return es[i].Distance < es[j].Distance
  })

  fmt.Printf("found %d events",len(es))


  const version string = "0.1"

  //what fields will be rendered to the table?
  sf :=  map[string]bool{}

  // Concrete Fields
  sf["EventType"] = true;
  sf["Name"] = true;
  sf["Description"] = false;
  sf["Url"] = true;
  sf["Organizer"] = true;
  sf["Virtual"] = true;
  sf["Location.Name"] = true;
  sf["Location.Country"] = true;
  sf["Location.Region"] = true;
  sf["Location.Locality"] = true;

  // Virtual Fields
  sf["_DistanceMiles"] = true;
  sf["_DistanceDays"] = true;
  sf["_Start_Date"] = false;
  sf["_Start_Month_Name"] = true;
  sf["_Start_Day"] = true;
  sf["_Start_Time"] = false;

  columns := []table.Column{}

  if (sf["EventType"]) {
    columns = append(columns,table.Column{Title: "Source", Width: 20})
  }
  if (sf["Name"]) {
    columns = append(columns,table.Column{Title: "Name", Width: 40})
  }
  if (sf["Location.Name"]) {
    columns = append(columns,table.Column{Title: "Where", Width: 20})
  }
  if (sf["Location.Region"]) {
    columns = append(columns,table.Column{Title: "State", Width: 2})
  }
  if (sf["Location.Locality"]) {
    columns = append(columns,table.Column{Title: "City", Width: 10})
  }
  if (sf["_DistanceMiles"]) {
    columns = append(columns,table.Column{Title: "Dist (m)", Width: 10})
  }
  if (sf["_DistanceDays"]) {
    columns = append(columns,table.Column{Title: "Days from now", Width: 10})
  }
  if (sf["_Start_Month_Name"]) {
    columns = append(columns,table.Column{Title: "Month", Width: 10})
  }

  rows := []table.Row{}
  for _, evt := range es {
    rows = append(rows,ToTableRow(evt,sf))
  }

  t := table.New(
    table.WithColumns(columns),
    table.WithRows(rows),
    table.WithFocused(true),
    table.WithHeight(30),
  )

  s := table.DefaultStyles()

  s.Header = s.Header.
    BorderStyle(lipgloss.NormalBorder()).
    BorderForeground(lipgloss.Color("240")).
    BorderBottom(true).
    Bold(false)
  s.Selected = s.Selected.
    Foreground(lipgloss.Color("229")).
    Background(lipgloss.Color("57")).
    Bold(false)

  t.SetStyles(s)

  return model{t,es,es,sf}

}

func (m model) Init() tea.Cmd {
  return nil;
}

func (m model) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
    // 'type  assertion'
    switch msg := msg.(type) {

    case tea.KeyMsg:

        switch msg.String() {

        case "d":
          sort.Slice(m.filterEvents, func(i, j int) bool {
            return m.filterEvents[i].Distance < m.filterEvents[j].Distance
          })
        case "D":
          sort.Slice(m.filterEvents, func(i, j int) bool {
            return m.filterEvents[i].Distance > m.filterEvents[j].Distance
          })
        case "t":
          sort.Slice(m.filterEvents, func(i, j int) bool {
            return m.filterEvents[i].DaysFromNow < m.filterEvents[j].DaysFromNow
          })
        case "T":
          sort.Slice(m.filterEvents, func(i, j int) bool {
            return m.filterEvents[i].DaysFromNow > m.filterEvents[j].DaysFromNow
          })
        case "ctrl+c", "q":
            return m, tea.Quit
        case "up", "k":
          m.table.MoveUp(1)
        case "down", "j":
          m.table.MoveDown(1)
        case "enter", " ":
          m.table.Blur()
        }

        m.table.SetRows(ToTableRows(m.filterEvents,m.showField))
    }

    // Return the updated model to the Bubble Tea runtime for processing.
    // Note that we're not returning a command.
    return m, nil
}

func (m model) View() string {

    // The header
    s := "Events near you!\n\n"

    s += baseStyle.Render(m.table.View())

    // The footer
    s += "\nPress q to quit.\n"

    // Send the UI for rendering
    return s

}
