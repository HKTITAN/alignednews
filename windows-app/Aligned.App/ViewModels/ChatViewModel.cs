using System.Collections.ObjectModel;
using System.Text;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.UI.Dispatching;
using Aligned.Core.Net;
using Aligned.Core.Net.Dto;

namespace Aligned.App.ViewModels;

public partial class ChatMessage : ObservableObject
{
    [ObservableProperty] private string _role = "user";
    [ObservableProperty] private string _content = "";
    public bool IsUser => Role == "user";
}

public partial class ChatViewModel : ObservableObject
{
    private readonly AlignedApi _api = new();
    private readonly DispatcherQueue _ui = DispatcherQueue.GetForCurrentThread();
    private CancellationTokenSource? _cts;

    public ObservableCollection<ChatMessage> Messages { get; } = new();

    [ObservableProperty] private string _input = "";
    [ObservableProperty] private bool _isStreaming;
    [ObservableProperty] private string? _errorMessage;

    [RelayCommand]
    public async Task SendAsync()
    {
        if (string.IsNullOrWhiteSpace(Input) || IsStreaming) return;
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

    [RelayCommand] public void Stop() => _cts?.Cancel();
}
