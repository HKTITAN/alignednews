using System.Numerics;
using Microsoft.UI;
using Microsoft.UI.Composition;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Hosting;
using Microsoft.UI.Xaml.Media;
using Windows.UI;

namespace Aligned.App.Controls;

/// <summary>
/// 3-line morphing icon per design/ALIGNED_DESIGN.md.
/// Mirror of android-app's MorphingIcon composable.
/// </summary>
public sealed class MorphingIcon : Control
{
    public static readonly DependencyProperty SpecProperty = DependencyProperty.Register(
        nameof(Spec), typeof(IconSpec), typeof(MorphingIcon),
        new PropertyMetadata(MorphingIcons.Menu, OnSpecChanged));

    public static readonly DependencyProperty IconBrushProperty = DependencyProperty.Register(
        nameof(IconBrush), typeof(Brush), typeof(MorphingIcon),
        new PropertyMetadata(new SolidColorBrush(Colors.Black), OnBrushChanged));

    public IconSpec Spec { get => (IconSpec)GetValue(SpecProperty); set => SetValue(SpecProperty, value); }
    public Brush IconBrush { get => (Brush)GetValue(IconBrushProperty); set => SetValue(IconBrushProperty, value); }

    private const float ViewBox = 14f;
    private Compositor? _comp;
    private ContainerVisual? _root;
    private CompositionLineGeometry[]? _lineGeoms;
    private ShapeVisual? _shape;
    private CompositionSpriteShape[]? _shapes;
    private IconSpec? _previous;

    public MorphingIcon()
    {
        IsHitTestVisible = false;
        Loaded += OnLoaded;
        SizeChanged += (_, __) => Layout();
    }

    private void OnLoaded(object sender, RoutedEventArgs e)
    {
        _comp = ElementCompositionPreview.GetElementVisual(this).Compositor;
        _root = _comp.CreateContainerVisual();
        _shape = _comp.CreateShapeVisual();
        _lineGeoms = new CompositionLineGeometry[3];
        _shapes = new CompositionSpriteShape[3];
        for (int i = 0; i < 3; i++)
        {
            var g = _comp.CreateLineGeometry();
            var sh = _comp.CreateSpriteShape(g);
            sh.StrokeThickness = 2f;
            sh.StrokeStartCap = CompositionStrokeCap.Round;
            sh.StrokeEndCap = CompositionStrokeCap.Round;
            sh.StrokeBrush = _comp.CreateColorBrush(GetStrokeColor());
            _shape.Shapes.Add(sh);
            _lineGeoms[i] = g;
            _shapes[i] = sh;
        }
        _root.Children.InsertAtTop(_shape);
        ElementCompositionPreview.SetElementChildVisual(this, _root);
        Layout();
        ApplySpec(Spec, animate: false);
    }

    private static void OnSpecChanged(DependencyObject d, DependencyPropertyChangedEventArgs e)
    {
        if (d is MorphingIcon icon && icon._comp != null)
            icon.ApplySpec((IconSpec)e.NewValue, animate: true);
    }

    private static void OnBrushChanged(DependencyObject d, DependencyPropertyChangedEventArgs e)
    {
        if (d is MorphingIcon icon && icon._shapes != null && icon._comp != null)
        {
            var c = icon.GetStrokeColor();
            foreach (var s in icon._shapes) s.StrokeBrush = icon._comp.CreateColorBrush(c);
        }
    }

    private Color GetStrokeColor() =>
        IconBrush is SolidColorBrush scb ? scb.Color : Colors.Black;

    private void Layout()
    {
        if (_shape is null) return;
        var size = (float)Math.Min(ActualWidth, ActualHeight);
        if (size <= 0) size = 24f;
        _shape.Size = new Vector2(size, size);
        // Center the icon in the control if width != height
        var off = new Vector3(
            (float)(ActualWidth - size) / 2f,
            (float)(ActualHeight - size) / 2f, 0);
        _shape.Offset = off;
    }

    private void ApplySpec(IconSpec spec, bool animate)
    {
        if (_comp is null || _lineGeoms is null || _shape is null) return;
        var size = _shape.Size.X;
        var unit = size / ViewBox;

        var sameGroup = animate && _previous?.RotationGroup != null
                        && _previous.RotationGroup == spec.RotationGroup;

        if (sameGroup)
        {
            // Rotate the entire shape visual; line endpoints stay.
            var prevDeg = _previous!.RotationDegrees;
            var nextDeg = ShortestRotation(prevDeg, spec.RotationDegrees);
            var anim = _comp.CreateScalarKeyFrameAnimation();
            anim.InsertKeyFrame(0f, prevDeg);
            anim.InsertKeyFrame(1f, nextDeg);
            anim.Duration = TimeSpan.FromMilliseconds(280);
            _shape.CenterPoint = new Vector3(size / 2f, size / 2f, 0);
            _shape.StartAnimation("RotationAngleInDegrees", anim);
        }
        else
        {
            // Cross-group morph — animate each endpoint.
            _shape.RotationAngleInDegrees = spec.RotationDegrees;
            _shape.CenterPoint = new Vector3(size / 2f, size / 2f, 0);
            for (int i = 0; i < 3; i++)
            {
                var line = spec.Lines[i];
                var startTarget = new Vector2(line.X1 * unit, line.Y1 * unit);
                var endTarget   = new Vector2(line.X2 * unit, line.Y2 * unit);
                if (!animate)
                {
                    _lineGeoms[i].Start = startTarget;
                    _lineGeoms[i].End = endTarget;
                    _shapes![i].StrokeThickness = line.Visible ? 2f : 0f;
                    continue;
                }
                var startAnim = _comp.CreateVector2KeyFrameAnimation();
                startAnim.InsertKeyFrame(1f, startTarget);
                startAnim.Duration = TimeSpan.FromMilliseconds(280);
                _lineGeoms[i].StartAnimation("Start", startAnim);

                var endAnim = _comp.CreateVector2KeyFrameAnimation();
                endAnim.InsertKeyFrame(1f, endTarget);
                endAnim.Duration = TimeSpan.FromMilliseconds(280);
                _lineGeoms[i].StartAnimation("End", endAnim);

                _shapes![i].StrokeThickness = line.Visible ? 2f : 0f;
            }
        }
        _previous = spec;
    }

    private static float ShortestRotation(float from, float targetRaw)
    {
        var delta = ((targetRaw - from) % 360f + 540f) % 360f - 180f;
        return from + delta;
    }
}

public sealed record IconLine(float X1, float Y1, float X2, float Y2, bool Visible = true)
{
    public static readonly IconLine Collapsed = new(7, 7, 7, 7, false);
}

public sealed record IconSpec(string Id, IReadOnlyList<IconLine> Lines, string? RotationGroup = null, float RotationDegrees = 0f)
{
    static IconSpec() { }
    public static IconSpec Make(string id, int[][] coords, string? rotGroup = null, float rotDeg = 0f)
    {
        if (coords.Length != 3) throw new ArgumentException("Need 3 lines", nameof(coords));
        var lines = new IconLine[3];
        for (int i = 0; i < 3; i++)
        {
            var c = coords[i];
            if (c[0] == 7 && c[1] == 7 && c[2] == 7 && c[3] == 7) lines[i] = IconLine.Collapsed;
            else lines[i] = new IconLine(c[0], c[1], c[2], c[3]);
        }
        return new IconSpec(id, lines, rotGroup, rotDeg);
    }
}

public static class MorphingIcons
{
    // arrow rotation group
    public static readonly IconSpec ArrowUp    = IconSpec.Make("arrow-up",    new[]{new[]{7,12,7,2}, new[]{3,6,7,2},  new[]{7,2,11,6}}, "arrow",   0);
    public static readonly IconSpec ArrowRight = IconSpec.Make("arrow-right", new[]{new[]{7,12,7,2}, new[]{3,6,7,2},  new[]{7,2,11,6}}, "arrow",  90);
    public static readonly IconSpec ArrowDown  = IconSpec.Make("arrow-down",  new[]{new[]{7,12,7,2}, new[]{3,6,7,2},  new[]{7,2,11,6}}, "arrow", 180);
    public static readonly IconSpec ArrowLeft  = IconSpec.Make("arrow-left",  new[]{new[]{7,12,7,2}, new[]{3,6,7,2},  new[]{7,2,11,6}}, "arrow", 270);

    // chevron rotation group
    public static readonly IconSpec ChevronUp    = IconSpec.Make("chevron-up",    new[]{new[]{3,9,7,5}, new[]{7,5,11,9}, new[]{7,7,7,7}}, "chevron",   0);
    public static readonly IconSpec ChevronRight = IconSpec.Make("chevron-right", new[]{new[]{3,9,7,5}, new[]{7,5,11,9}, new[]{7,7,7,7}}, "chevron",  90);
    public static readonly IconSpec ChevronDown  = IconSpec.Make("chevron-down",  new[]{new[]{3,9,7,5}, new[]{7,5,11,9}, new[]{7,7,7,7}}, "chevron", 180);
    public static readonly IconSpec ChevronLeft  = IconSpec.Make("chevron-left",  new[]{new[]{3,9,7,5}, new[]{7,5,11,9}, new[]{7,7,7,7}}, "chevron", 270);

    // cross rotation group
    public static readonly IconSpec Plus  = IconSpec.Make("plus",  new[]{new[]{2,7,12,7}, new[]{7,2,7,12}, new[]{7,7,7,7}}, "cross",  0);
    public static readonly IconSpec Close = IconSpec.Make("close", new[]{new[]{2,7,12,7}, new[]{7,2,7,12}, new[]{7,7,7,7}}, "cross", 45);

    // singletons (matches android-app/src/main/java/ai/aligned/ui/icons/MorphingIcon.kt)
    public static readonly IconSpec Menu     = IconSpec.Make("menu",     new[]{new[]{2,4,12,4},  new[]{2,7,12,7},  new[]{2,10,12,10}});
    public static readonly IconSpec Check    = IconSpec.Make("check",    new[]{new[]{2,8,6,12},  new[]{6,12,12,3}, new[]{7,7,7,7}});
    public static readonly IconSpec Search   = IconSpec.Make("search",   new[]{new[]{3,3,9,3},   new[]{3,3,3,9},   new[]{7,7,12,12}});
    public static readonly IconSpec Settings = IconSpec.Make("settings", new[]{new[]{4,2,4,8},   new[]{7,5,7,12},  new[]{10,2,10,10}});
    public static readonly IconSpec Share    = IconSpec.Make("share",    new[]{new[]{7,12,7,2},  new[]{3,5,7,2},   new[]{7,2,11,5}});
    public static readonly IconSpec Bookmark = IconSpec.Make("bookmark", new[]{new[]{4,2,4,12},  new[]{10,2,10,12},new[]{4,12,10,12}});
    public static readonly IconSpec Play     = IconSpec.Make("play",     new[]{new[]{4,3,4,11},  new[]{4,3,11,7},  new[]{4,11,11,7}});
    public static readonly IconSpec Pause    = IconSpec.Make("pause",    new[]{new[]{5,3,5,11},  new[]{9,3,9,11},  new[]{7,7,7,7}});
    public static readonly IconSpec Sun      = IconSpec.Make("sun",      new[]{new[]{2,7,12,7},  new[]{7,2,7,12},  new[]{4,4,10,10}});
    public static readonly IconSpec Moon     = IconSpec.Make("moon",     new[]{new[]{9,3,5,7},   new[]{5,7,9,11},  new[]{9,3,11,7}});
    public static readonly IconSpec Refresh  = IconSpec.Make("refresh",  new[]{new[]{3,4,11,4},  new[]{11,4,11,10},new[]{11,10,7,10}});
    public static readonly IconSpec Sparkle  = IconSpec.Make("sparkle",  new[]{new[]{7,1,7,13},  new[]{1,7,13,7},  new[]{7,7,7,7}});
    public static readonly IconSpec Mic      = IconSpec.Make("mic",      new[]{new[]{5,3,5,9},   new[]{9,3,9,9},   new[]{4,12,10,12}});
    public static readonly IconSpec Send     = IconSpec.Make("send",     new[]{new[]{3,4,12,7},  new[]{3,10,12,7}, new[]{3,7,8,7}});
    public static readonly IconSpec Flame    = IconSpec.Make("flame",    new[]{new[]{4,12,7,3},  new[]{10,12,7,3}, new[]{4,12,10,12}});
    public static readonly IconSpec Globe    = IconSpec.Make("globe",    new[]{new[]{2,7,12,7},  new[]{7,2,7,12},  new[]{3,4,11,4}});

    // v0.2 additions
    public static readonly IconSpec Heart    = IconSpec.Make("heart",    new[]{new[]{3,5,7,9},   new[]{11,5,7,9},  new[]{2,7,12,7}});
    public static readonly IconSpec Retweet  = IconSpec.Make("retweet",  new[]{new[]{2,4,12,4},  new[]{10,2,12,4}, new[]{2,10,4,12}});
    public static readonly IconSpec Reply    = IconSpec.Make("reply",    new[]{new[]{2,7,6,3},   new[]{2,7,6,11},  new[]{2,7,12,7}});
    public static readonly IconSpec Eye      = IconSpec.Make("eye",      new[]{new[]{2,7,12,7},  new[]{4,4,10,10}, new[]{7,5,7,9}});
    public static readonly IconSpec Calendar = IconSpec.Make("calendar", new[]{new[]{2,4,12,4},  new[]{2,4,2,12},  new[]{12,4,12,12}});
    public static readonly IconSpec Clock    = IconSpec.Make("clock",    new[]{new[]{7,7,7,3},   new[]{7,7,10,7},  new[]{2,7,12,7}});
    public static readonly IconSpec Pin      = IconSpec.Make("pin",      new[]{new[]{7,2,7,12},  new[]{4,5,10,5},  new[]{5,8,9,8}});
    public static readonly IconSpec Bell     = IconSpec.Make("bell",     new[]{new[]{4,10,7,3},  new[]{10,10,7,3}, new[]{4,10,10,10}});
    public static readonly IconSpec History  = IconSpec.Make("history",  new[]{new[]{2,7,7,2},   new[]{7,2,12,7},  new[]{7,7,7,7}});
}
