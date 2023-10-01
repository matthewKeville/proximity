package grid

import (
  "proximity-client/http"
  "proximity-client/event"
  "fmt"
  "log"
  "sort"
  "github.com/charmbracelet/bubbletea"
  "github.com/evertras/bubble-table/table"
  "github.com/charmbracelet/lipgloss"
)

const (
  columnKeyID               = "id"
  columnKeyType             = "type"
  columnKeyOnline           = "online"
  columnKeyName             = "name"
  columnKeyDistance         = "distance"
  columnKeyRegion           = "region"
  columnKeyLocality         = "locality"
  columnKeyMonth            = "month"
  columnKeyDayOfWeek        = "dayOfweek"
  columnKeyDaysFromNow      = "daysFromNow"
  columnKeyDate             = "date"
  columnKeyTime             = "time"
)

type model struct {
  table table.Model
  events []event.Event
  filterEvents []event.Event
}


func toTableRows(evs []event.Event) []table.Row {
  rows := []table.Row{}
  for _, evt := range evs {
    rows = append(rows,toTableRow(evt))
  }
  return rows
}

// green -> yellow ->  orange -> red
func colorEventDistance(dist float32) string {
  if ( dist < 1.0 ) {
    return "46"
  } else if ( dist < 3.0 ) {
    return "154"
  } else if ( dist < 5.0 ) {
    return "226"
  } else if ( dist < 10.0 ) {
    return "220"
  } else if ( dist < 30.0 ) {
    return "166"
  } else {
    return "196"
  }
}

func colorEventDaysFromNow(days int) string {
  if ( days < 0 ) {
    return "016"
  } else if ( days <= 1 ) {
    return "46"
  } else if ( days < 3 ) {
    return "154"
  } else if ( days < 5 ) {
    return "226"
  } else if ( days < 10 ) {
    return "220"
  } else if ( days < 30 ) {
    return "166"
  } else {
    return "196"
  }
}

func colorEventType(t string) string {
  switch t  {
  case "ALLEVENTS":
    return  "051" //light blue
  case "EVENTBRITE":
    return  "208" //orange
  case "MEETUP":
    return  "160" //red
  }
  return "0"
}

func colorDayOfWeek(t string) string {
  switch t  {
  case "Monday":
    return "1"
  case "Tuesday":
    return "2"
  case "Wednesday":
    return "3"
  case "Thursday":
    return "4"
  case "Friday":
    return "5"
  case "Saturday":
    return "6"
  case "Sunday":
    return "8"
  }
  return "0"
}

func colorMonth(t string) string {

  winter := "030"
  spring := "090"
  summer := "142"
  fall   := "094"

  switch t  {
  case "January":
    return winter
  case "February":
    return winter
  case "March":
    return winter
  case "April":
    return spring
  case "May":
    return spring
  case "June":
    return spring
  case "July":
    return summer
  case "August":
    return summer
  case "September":
    return summer
  case "October":
    return fall
  case "November":
    return fall
  case "December":
    return fall
  }
  return "0"

}

func colorVirtual(v bool) string {
  if ( v ) {
    return "160"
  } else {
    return "046"
  }
}

func toTableRow(e event.Event) table.Row {
  return table.NewRow(table.RowData{
    columnKeyType: table.NewStyledCell(
      e.EventType, lipgloss.NewStyle().
        Foreground(lipgloss.Color(colorEventType(e.EventType)))),
    columnKeyOnline: table.NewStyledCell(
      e.Virtual, lipgloss.NewStyle().
        Foreground(lipgloss.Color(colorVirtual(e.Virtual)))),
    columnKeyDistance: table.NewStyledCell(
      fmt.Sprintf("%.2f",e.Distance), lipgloss.NewStyle().
        Foreground(lipgloss.Color(colorEventDistance(e.Distance)))),
    columnKeyDaysFromNow: table.NewStyledCell(
      e.DaysFromNow, lipgloss.NewStyle().
        Foreground(lipgloss.Color(colorEventDaysFromNow(e.DaysFromNow)))),
    columnKeyName: e.Name,
    //columnKeyMonth: fmt.Sprintf("%s",e.Start.Month()),
    columnKeyMonth: table.NewStyledCell(
      e.Start.Month(), lipgloss.NewStyle().
        Foreground(lipgloss.Color(colorMonth(e.Start.Month().String())))),
    columnKeyDayOfWeek: table.NewStyledCell(
      e.Start.Weekday(), lipgloss.NewStyle().
        Foreground(lipgloss.Color(colorDayOfWeek(e.Start.Weekday().String())))),
    columnKeyRegion: e.Location.Region,
    columnKeyLocality: e.Location.Locality,
    columnKeyDate:  fmt.Sprintf("%d/%d/%d",e.Start.Month(),e.Start.Day(),e.Start.Year()),
    columnKeyTime:  fmt.Sprintf("%d:%d",e.Start.Local().Hour(),e.Start.Local().Minute()),
  })
}

func InitialModel(latitude float64,longitude float64,radius float64,showVirtual bool,daysBefore int) model {

  log.Printf("latitude : %f\tlongitude : %f\tradius : %f",latitude,longitude,radius);

  es := http.GetEvents(latitude,longitude,radius,showVirtual,daysBefore)


  sort.Slice(es, func(i, j int) bool {
    return es[i].Distance < es[j].Distance
  })

  log.Printf("found %d events",len(es))


  const version string = "0.1"

  columnHeaderStyle := 
    lipgloss.NewStyle().
    Foreground(lipgloss.Color("231")).
    Bold(true).
    Align(lipgloss.Center)
  
  columns := []table.Column{
    table.NewColumn(columnKeyType, "TYPE", 12),
    table.NewColumn(columnKeyOnline, "ONLINE", 8),
    table.NewColumn(columnKeyDistance, "DIST", 6),
    table.NewColumn(columnKeyDaysFromNow, "IN DAYS", 7),
    table.NewColumn(columnKeyName, "NAME", 80),
    table.NewColumn(columnKeyRegion, "STATE", 5),
    table.NewColumn(columnKeyLocality, "CITY", 20),
    table.NewColumn(columnKeyMonth, "MONTH", 9),
    table.NewColumn(columnKeyDayOfWeek, "DAY OF WEEK", 9),
    table.NewColumn(columnKeyDate, "DATE", 10),
    table.NewColumn(columnKeyTime, "TIME", 5),

  }

  rows := toTableRows(es)

  keys := table.DefaultKeyMap()
  keys.RowDown.SetKeys("j", "down")
  keys.RowUp.SetKeys("k", "up")

  t := table.New(columns).
    WithRows(rows).
    HeaderStyle(columnHeaderStyle).
    Focused(true).
    WithKeyMap(keys).
    WithStaticFooter("A bird in the hand is worth two in the bush.").
    WithPageSize(20).
    WithBaseStyle(
      lipgloss.NewStyle().
      BorderForeground(lipgloss.Color("#a38")).
      Foreground(lipgloss.Color("#a7a")).
      Align(lipgloss.Left),
      ).
    SortByAsc(columnKeyDaysFromNow).
    WithMissingDataIndicatorStyled(table.StyledCell{
      Style: lipgloss.NewStyle().Foreground(lipgloss.Color("#faa")),
      Data:  "😕",
    })

  return model{t,es,es}

}

func (m model) Init() tea.Cmd {
  return nil;
}

func (m model) Update(msg tea.Msg) (tea.Model, tea.Cmd) {

    var (
      cmd tea.Cmd
      cmds []tea.Cmd
    )

    m.table, cmd  = m.table.Update(msg)
    cmds = append(cmds, cmd)

    // 'type  assertion'
    switch msg := msg.(type) {

    case tea.KeyMsg:

        switch msg.String() {

        case "t":
          m.table = m.table.SortByAsc(columnKeyDaysFromNow)
        case "T":
          m.table = m.table.SortByDesc(columnKeyDaysFromNow)
        case "d":
          m.table = m.table.SortByAsc(columnKeyDistance)
        case "D":
          m.table = m.table.SortByDesc(columnKeyDistance)

        case "ctrl+c", "q":
            return m, tea.Quit
        }

    }


    return m, tea.Batch(cmds...)
}

func (m model) View() string {

    s := "Events near you!\n\n"

    s += m.table.View()

    return s

}
