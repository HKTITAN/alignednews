using System.Collections.ObjectModel;
using System.Text;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.UI.Dispatching;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Media;
using Aligned.App.Controls;
using Aligned.Core.Net;
using Aligned.Core.Net.Dto;

namespace Aligned.App.ViewModels;

public partial class ChatMessage : ObservableObject
{
    [ObservableProperty] private string _role = "user";
    [ObservableProperty] private string _content = "";

    public bool IsUser => Role == "user";
    public HorizontalAlignment BubbleAlignment =>
        IsUser ? HorizontalAlignment.Right : HorizontalAlignment.Left;

    public Brush BubbleBrush =>
        IsUser ? (Brush)Application.Current.Resources["AlignedAccent"]
               : (Brush)Application.Current.Resources["AlignedElev1"];
    public Brush BubbleTextBrush =>
        IsUser ? (Brush)Application.Current.Resources["AlignedOnAccent"]
               : (Brush)Application.Current.Resources["AlignedText"];

    public CornerRadius BubbleCorner =>
        IsUser ? new CornerRadius(18, 18, 4, 18)
               : new CornerRadius(18, 18, 18, 4);

    partial void OnRoleChanged(string value)
    {
        OnPropertyChanged(nameof(IsUser));
        OnPropertyChanged(nameof(BubbleAlignment));
        OnPropertyChanged(nameof(BubbleBrush));
        OnPropertyChanged(nameof(BubbleTextBrush));
        OnPropertyChanged(nameof(BubbleCorner));
    }
}

public partial class SuggestionItem : ObservableObject
{
    public string Text { get; }
    public IRelayCommand PickCommand { get; }
    public SuggestionItem(string text, Action<string> onPick)
    {
        Text = text;
        PickCommand = new RelayCommand(() => onPick(text));
    }
}

public partial class ChatViewModel : ObservableObject
{
    private readonly AlignedApi _api = new();
    private readonly DispatcherQueue _ui = DispatcherQueue.GetForCurrentThread();
    private CancellationTokenSource? _cts;

    public ObservableCollection<ChatMessage> Messages { get; } = new();

    public ObservableCollection<SuggestionItem> Suggestions { get; } = new();

    [ObservableProperty] private string _input = "";
    [ObservableProperty] private bool _isStreaming;
    [ObservableProperty] private string? _errorMessage;

    public ChatViewModel()
    {
        foreach (var s in new[]
        {
            "What's the biggest AI safety story today?",
            "Summarize today's xAI news",
            "Which companies announced new models?",
            "What's happening in robotics?",
            "Any major research papers worth reading?"
        }) Suggestions.Add(new SuggestionItem(s, t => { Input = t; _ = SendAsync(); }));
        Messages.CollectionChanged += (_, __) => NotifyChromeChanged();
    }

    public string SubtitleLine =>
        IsStreaming ? "Streaming reply…"
        : Messages.Count == 0 ? "Ground-truth grounded in today's stories"
        : $"{Messages.Count(m => m.IsUser)} questions this session";

    public Visibility EmptyVisibility =>
        Messages.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ListVisibility =>
        Messages.Count > 0 ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ClearVisibility =>
        Messages.Count > 0 ? Visibility.Visible : Visibility.Collapsed;

    public Brush SendButtonBrush =>
        IsStreaming ? (Brush)Application.Current.Resources["AlignedText"]
        : !string.IsNullOrWhiteSpace(Input) ? (Brush)Application.Current.Resources["AlignedAccent"]
        : (Brush)Application.Current.Resources["AlignedSurface"];

    public Brush SendIconBrush =>
        IsStreaming ? (Brush)Application.Current.Resources["AlignedBg"]
        : !string.IsNullOrWhiteSpace(Input) ? (Brush)Application.Current.Resources["AlignedOnAccent"]
        : (Brush)Application.Current.Resources["AlignedTextTertiary"];

    public IconSpec SendIcon =>
        IsStreaming ? MorphingIcons.Pause : MorphingIcons.Send;

    partial void OnInputChanged(string value)
    {
        SendCommand.NotifyCanExecuteChanged();
        OnPropertyChanged(nameof(SendButtonBrush));
        OnPropertyChanged(nameof(SendIconBrush));
    }
    partial void OnIsStreamingChanged(bool value)
    {
        OnPropertyChanged(nameof(SubtitleLine));
        OnPropertyChanged(nameof(SendButtonBrush));
        OnPropertyChanged(nameof(SendIconBrush));
        OnPropertyChanged(nameof(SendIcon));
    }

    private void NotifyChromeChanged()
    {
        OnPropertyChanged(nameof(EmptyVisibility));
        OnPropertyChanged(nameof(ListVisibility));
        OnPropertyChanged(nameof(ClearVisibility));
        OnPropertyChanged(nameof(SubtitleLine));
    }

    public bool CanSend => !IsStreaming && !string.IsNullOrWhiteSpace(Input);

    [RelayCommand(CanExecute = nameof(CanSend))]
    public async Task SendAsync()
    {
        if (!CanSend) return;
        var prompt = Input.Trim();
        Input = "";
        Messages.Add(new ChatMessage { Role = "user", Content = prompt });
        var reply = new ChatMessage { Role = "assistant", Content = "" };
        Messages.Add(reply);

        var history = Messages.Take(Messages.Count - 1)
                              .Where(m => !string.IsNullOrEmpty(m.Content))
                              .Select(m => new ChatTurn(m.Role, m.Content))
                              .ToList();

        IsStreaming = true;
        ErrorMessage = null;
        _cts = new CancellationTokenSource();
        var buf = new StringBuilder();
        try
        {
            await foreach (var evt in _api.Chat(prompt, history: history, ct: _cts.Token))
            {
                if (evt is ChatEvent.Token t)
                {
                    buf.Append(t.Text);
                    var snapshot = buf.ToString();
                    _ui.TryEnqueue(() => reply.Content = snapshot);
                }
            }
        }
        catch (OperationCanceledException) { /* expected */ }
        catch (Exception ex) { ErrorMessage = ex.Message; }
        finally
        {
            IsStreaming = false;
            _cts?.Dispose();
            _cts = null;
        }
    }

    public void Stop() => _cts?.Cancel();

    [RelayCommand]
    public void Clear()
    {
        _cts?.Cancel();
        Messages.Clear();
        ErrorMessage = null;
    }
}
