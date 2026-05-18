I've been experimenting more with [Claude Code](https://claude.ai/code) lately, trying to push the limits of its animation and craft skills. One thing I've always loved are the small moments in interfaces where icons transform rather than swap. The hamburger menu that rotates into an . The play button that becomes pause. These transitions feel considered in a way that static changes don't.

I wanted to see if Claude could help me build something like this, but with a twist: every icon should be able to become *any* other icon. Not through crossfades or opacity tricks, but through actual transformation of the underlying shapes.

click to cycle

This is what I ended up with. Twenty-one icons, any of which can morph into any other. The whole thing came together in a single session with Claude Code.

---

## Setting constraints

The first attempt was predictable. I asked Claude for an icon component with smooth transitions, and it gave me an `AnimatePresence` wrapper that crossfades between SVGs. Technically correct, but not what I had in mind. The icons fade out and fade in. There's no sense of *transformation*.

I wanted the lines themselves to move. The three lines of a hamburger sliding and rotating into an .

Every icon should use exactly ==three SVG lines==. Icons that need fewer collapse the extras to invisible points. This means any icon can morph into any other, since they share the same underlying structure.

the key insight

I gave Claude this constraint and it ran with it. Every icon, regardless of complexity, would be represented by exactly three lines, with unused lines collapsing to invisible center points.<sup><a href="#user-content-fn-1">1</a></sup>

Some icons mapped naturally. Menu is three horizontal bars. Plus is vertical and horizontal with the third collapsed. Cross is just plus rotated 45°. Others required more creativity. The checkmark uses two lines for its legs and collapses the third. Play is three lines forming a triangle, each sharing endpoints with the others.<sup><a href="#user-content-fn-2">2</a></sup>

---

## Rotation groups

With the icons defined, I started clicking through transitions. Most looked good, but arrow-right to arrow-down looked janky. The lines were morphing coordinates when they should have been rotating.

This is the sort of thing you only notice by playing with it. Arrow-right and arrow-down are the same shape, just rotated 90°. When you morph coordinates, the lines bend and warp. When you rotate, it just works.

I described this to Claude and we introduced the concept of rotation groups. Icons in the same group share coordinates and differ only by rotation. Arrows rotate in 90° increments. Chevrons too. Plus and cross are the same shape, 45° apart.

Arrows

Four directions, 90° apart.

Chevrons

Same shape as arrows, without the shaft.

Plus / Cross

The same perpendicular lines, 45° apart.

Now an arrow pointing right *rotates* to point down.

---

## Cross-group morphs

When transitioning between different groups, the lines interpolate through coordinate space. [Motion](https://motion.dev) handles the tweening. These are the transitions that feel most magical. You're watching shapes genuinely transform into other shapes.

Arrow-to-check is my favourite.<sup><a href="#user-content-fn-3">3</a></sup>

---

I asked Claude to build this sequencer during development in order to test transitions, which proved to be an incredibly helpful tool for feedback. I could simply point to specific sequences that felt off and explain why.

Click icons to add/remove · Click preview to cycle

---

## Reflections

I'm genuinely impressed with what Claude was able to build here. The core architecture is elegant and extensible: three lines per icon, rotation groups for same-shape icons, coordinate morphing for everything else.

With that said, there were limitations. Claude couldn't tell when something looked wrong. The insight that arrows should rotate rather than morph coordinates, for example, had to come from me, even though rotating would have meant smoother, more natural animations. It optimised for *working* rather than *feeling right*, which meant I had to watch the transitions and describe what felt off. Once I did, it started to understand the why.

The underlying approach is sound though. If you want to try something similar, here's a general prompt to get started:

Build an icon component where any icon can smoothly morph into any other. Every icon should use exactly three SVG lines. Icons that need fewer lines collapse the extras to invisible center points. For icons that are the same shape at different rotations (like arrows), use rotation instead of coordinate morphing. Don't make mistakes plz!

From there, it's iteration. Play with the result, notice what's wrong, describe it, repeat.

[^1]: A collapsed line is just a point at the center: `{ x1: 7, y1: 7, x2: 7, y2: 7 }`. With `opacity: 0` so you don't see a dot.

[^2]: When play morphs to pause, one line collapses while the other two separate into parallel bars.

[^3]: With hundreds of possible icon pairs, not every transition takes the ideal path. Some morphs have slightly awkward intermediate states, and some rotations overshoot. Nothing that couldn't be fixed with more iteration.