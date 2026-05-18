using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.UI.Dispatching;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Media;
using Windows.UI;
using Aligned.App.Theming;
using Aligned.Core.Net;
using Aligned.Core.Net.Dto;

namespace Aligned.App.ViewModels;

public class ResearchStepVm
{
    public string Name { get; set; } = "";
    public string Status { get; set; } = "";
    public string Detail { get; set; } = "";
    public string StatusGlyph => Status switch
    {
        "complete" => "●",
        "running"  => "◐",
        _          => "○"
    };
    public Brush StatusBrush => Status switch
    {
        "complete" => new SolidColorBrush(CategoryPalette.FromHex("#34C759")),
        "running"  => new SolidColorBrush(CategoryPalette.FromHex("#0A84FF")),
        _          => new SolidColorBrush(CategoryPalette.FromHex("#86868B"))
    };
}

public class InsightVm
{
    public string Title { get; set; } = "";
    public string Summary { get; set; } = "";
    public int Confidence { get; set; }
    public string ConfidenceLabel => Confidence > 0 ? $"Confidence {Confidence}%" : "";
}

public partial class ResearchViewModel : ObservableObject
{
    private readonly AlignedApi _api = new();
    private readonly DispatcherQueue _ui = DispatcherQueue.GetForCurrentThread();
    private CancellationTokenSource? _poll;

    [ObservableProperty] private string _query = "";
    [ObservableProperty] private string? _researchId;
    [ObservableProperty] private string _status = "idle";
    [ObservableProperty] private double _progress;
    [ObservableProperty] private string? _answer;
    [ObservableProperty] private string? _errorMessage;
    [ObservableProperty] private bool _isRunning;

    public ObservableCollection<ResearchStepVm> Steps { get; } = new();
    public ObservableCollection<InsightVm> Insights { get; } = new();

    public string SubtitleLine =>
        Status switch
        {
            "running"  => "Researching…",
            "complete" => "Complete",
            _          => "Multi-step deep research powered by /api/research"
        };

    public Color StatusColor =>
        Status switch
        {
            "running"  => CategoryPalette.FromHex("#0A84FF"),
            "complete" => CategoryPalette.FromHex("#34C759"),
            _          => CategoryPalette.FromHex("#86868B")
        };

    public string StatusLine =>
        Status switch
        {
            "running"  => $"Step {Steps.Count(s => s.Status == "complete")} / {Math.Max(Steps.Count, 10)}",
            "complete" => "All steps complete",
            _          => "Awaiting query"
        };

    public bool IsIndeterminate => IsRunning && Steps.Count == 0;

    public Visibility HintVisibility => Steps.Count == 0 && Answer == null && !IsRunning ? Visibility.Visible : Visibility.Collapsed;
    public Visibility StepsVisibility => Steps.Count > 0 || IsRunning ? Visibility.Visible : Visibility.Collapsed;
    public Visibility AnswerVisibility => !string.IsNullOrWhiteSpace(Answer) ? Visibility.Visible : Visibility.Collapsed;

    partial void OnStatusChanged(string value)
    {
        OnPropertyChanged(nameof(SubtitleLine));
        OnPropertyChanged(nameof(StatusColor));
        OnPropertyChanged(nameof(StatusLine));
    }
    partial void OnAnswerChanged(string? value) => OnPropertyChanged(nameof(AnswerVisibility));
    partial void OnIsRunningChanged(bool value)
    {
        OnPropertyChanged(nameof(HintVisibility));
        OnPropertyChanged(nameof(StepsVisibility));
        OnPropertyChanged(nameof(IsIndeterminate));
    }

    public bool CanStart => !IsRunning && !string.IsNullOrWhiteSpace(Query);

    [RelayCommand(CanExecute = nameof(CanStart))]
    public async Task StartAsync()
    {
        if (!CanStart) return;
        _poll?.Cancel();
        Steps.Clear();
        Insights.Clear();
        Answer = null;
        ErrorMessage = null;
        Status = "running";
        Progress = 0;
        IsRunning = true;
        try
        {
            var resp = await _api.StartResearch(Query.Trim());
            ResearchId = resp?.Id;
            if (string.IsNullOrEmpty(ResearchId)) throw new InvalidOperationException("Server did not return a research id");
            _poll = new CancellationTokenSource();
            _ = Task.Run(() => PollLoop(ResearchId!, _poll.Token));
        }
        catch (Exception ex)
        {
            ErrorMessage = ex.Message;
            IsRunning = false;
            Status = "error";
        }
    }

    private async Task PollLoop(string id, CancellationToken ct)
    {
        try
        {
            while (!ct.IsCancellationRequested)
            {
                var r = await _api.Research(id);
                if (r != null)
                {
                    _ui.TryEnqueue(() => ApplySnapshot(r));
                    if (r.Status == "complete")
                    {
                        _ui.TryEnqueue(() => { IsRunning = false; Status = "complete"; Progress = 100; });
                        return;
                    }
                }
                try { await Task.Delay(3500, ct); } catch { return; }
            }
        }
        catch (Exception ex)
        {
            _ui.TryEnqueue(() => { ErrorMessage = ex.Message; IsRunning = false; Status = "error"; });
        }
    }

    private void ApplySnapshot(ResearchDto r)
    {
        Steps.Clear();
        foreach (var s in r.Steps)
            Steps.Add(new ResearchStepVm { Name = s.Name, Status = s.Status, Detail = s.Detail ?? "" });
        Insights.Clear();
        foreach (var i in r.Insights)
            Insights.Add(new InsightVm { Title = i.Title, Summary = i.Summary, Confidence = i.Confidence });
        Answer = r.SummaryAnswer;
        if (r.Steps.Count > 0)
        {
            int done = r.Steps.Count(s => s.Status == "complete");
            Progress = 100.0 * done / Math.Max(r.Steps.Count, 1);
        }
        OnPropertyChanged(nameof(StatusLine));
    }

    partial void OnQueryChanged(string value) => StartCommand.NotifyCanExecuteChanged();
}
