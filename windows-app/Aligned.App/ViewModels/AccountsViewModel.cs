using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.UI.Xaml;
using Windows.System;
using Aligned.App.Theming;
using Aligned.Core.Net;
using Aligned.Core.Net.Dto;

namespace Aligned.App.ViewModels;

public partial class AccountVm : ObservableObject
{
    public string Id { get; set; } = "";
    public string Name { get; set; } = "";
    public string Username { get; set; } = "";
    public string ProfileImage { get; set; } = "";
    public string Description { get; set; } = "";
    public long Followers { get; set; }
    public string FollowersLabel => Followers switch
    {
        < 1_000 => $"{Followers} followers",
        < 1_000_000 => $"{Followers / 1_000}K followers",
        _ => $"{Followers / 100_000 / 10.0:0.0}M followers"
    };
    public IRelayCommand OpenCommand { get; }
    public AccountVm()
    {
        OpenCommand = new RelayCommand(async () =>
            await Launcher.LaunchUriAsync(new Uri($"https://x.com/{Username}")));
    }
}

public partial class AccountsViewModel : ObservableObject
{
    private readonly AlignedApi _api = new();

    public ObservableCollection<AccountVm> Accounts { get; } = new();

    [ObservableProperty] private bool _isLoading;
    [ObservableProperty] private string? _errorMessage;

    public Visibility LoadingVisibility => IsLoading && Accounts.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ErrorVisibility => !IsLoading && ErrorMessage != null && Accounts.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
    public Visibility ContentVisibility => Accounts.Count > 0 ? Visibility.Visible : Visibility.Collapsed;
    public string Subtitle => IsLoading ? "Loading…" : $"{Accounts.Count} curated accounts";

    public AccountsViewModel() => _ = LoadAsync();

    partial void OnIsLoadingChanged(bool value) => Notify();
    private void Notify()
    {
        OnPropertyChanged(nameof(LoadingVisibility));
        OnPropertyChanged(nameof(ErrorVisibility));
        OnPropertyChanged(nameof(ContentVisibility));
        OnPropertyChanged(nameof(Subtitle));
    }

    [RelayCommand]
    public async Task LoadAsync()
    {
        IsLoading = true;
        ErrorMessage = null;
        try
        {
            var dto = await WarmCache.GetOrFetch("accounts", WarmCache.ColdTtl, () => _api.Accounts());
            Accounts.Clear();
            foreach (var a in (dto?.Accounts ?? new()).OrderByDescending(a => a.Followers))
            {
                Accounts.Add(new AccountVm
                {
                    Id = a.Id, Name = a.Name, Username = a.Username,
                    ProfileImage = a.ProfileImage, Description = a.Description,
                    Followers = a.Followers
                });
            }
        }
        catch (Exception ex) { ErrorMessage = ex.Message; }
        finally { IsLoading = false; Notify(); }
    }
}
