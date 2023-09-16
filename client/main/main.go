package main

import (
  "proximity-client/http"
  "proximity-client/event"
  "os"
  "fmt"
  "github.com/charmbracelet/bubbletea"
  "github.com/charmbracelet/bubbles/table"
  "github.com/charmbracelet/lipgloss"
)


var baseStyle = lipgloss.NewStyle().
  BorderStyle(lipgloss.NormalBorder()).
  BorderForeground(lipgloss.Color("240"))

type model struct {
  table table.Model
}

func ToTableRow(e event.Event) table.Row {
    return table.Row{
      e.EventType,
      fmt.Sprintf("%d",(e.Id)),
      e.EventId,
      e.Name,
      e.Description,
      e.Start.String(),
      e.Location.Region,
      e.Location.Locality,
      fmt.Sprintf("%f",(e.Location.Latitude)),
      fmt.Sprintf("%f",(e.Location.Longitude)),
      e.Url,
      e.Organizer,
      fmt.Sprintf("%t",e.Virtual),
    }
   
}


func main() {

  rows := []table.Row{}

  es := http.GetEvents()
  for _, evt := range es {
    rows = append(rows,ToTableRow(evt))
  }

  const version string = "0.1"

  columns := []table.Column{
    {Title: "Type", Width: 10},
    {Title: "Id", Width: 10},
    {Title: "EvId", Width: 10},
    {Title: "Name", Width: 20},
    {Title: "Description", Width: 20},
    {Title: "Start", Width: 10},
    {Title: "Region", Width: 10},
    {Title: "Locality", Width: 10},
    {Title: "Lat", Width: 10},
    {Title: "Lon", Width: 10},
    {Title: "Url", Width: 10},
    {Title: "Org", Width: 10},
    {Title: "Online", Width: 10},
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

  m := model{t}

  p := tea.NewProgram(m)
  if _, err := p.Run(); err != nil {
    fmt.Printf("An error occurred, error : %v",err)
    os.Exit(1)
  }

}


func (m model) Init() tea.Cmd {
  return nil;
}

func (m model) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
    // 'type  assertion'
    switch msg := msg.(type) {

    case tea.KeyMsg:

        switch msg.String() {

        case "ctrl+c", "q":
            return m, tea.Quit

        case "up", "k":
          m.table.MoveUp(1)
        case "down", "j":
          m.table.MoveDown(1)
        case "enter", " ":
          m.table.Blur()

        }
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
