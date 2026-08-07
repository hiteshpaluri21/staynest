# Landing page photography

Photographs used by `src/pages/LandingPage.jsx` for the hero, the four room types, the four
dining outlets and the two carousel slides.

They are committed to the repo rather than hot-linked, so the public page renders the same
whether or not the machine has internet, and a third party going down cannot break it. Each
was fetched at `w=900` (`w=1400` for the hero) and `q=72`, which is why they are ~100–270 KB
rather than several megabytes.

## Source and licence

All nine come from [Unsplash](https://unsplash.com) under the
[Unsplash Licence](https://unsplash.com/license): free to use commercially, no attribution
required, no permission needed. None are Unsplash+ (the paid tier), which carries different
terms — that was checked for each one.

Identified by Unsplash photo id where known, otherwise by the CDN image id.

| File | Unsplash photo | Shows |
| --- | --- | --- |
| `hero-hotel.jpg` | [`_pPHgeHz1uk`](https://unsplash.com/photos/_pPHgeHz1uk) | Resort pool at dusk, palms and loungers |
| `room-standard.jpg` | [`p3UWyaujtQo`](https://unsplash.com/photos/p3UWyaujtQo) | Hotel bedroom, warm lamp light |
| `room-deluxe.jpg` | [`Yrxr3bsPdS0`](https://unsplash.com/photos/Yrxr3bsPdS0) | King bed, bedside lamps, curtained window |
| `room-suite.jpg` | [`OtXADkUh3-I`](https://unsplash.com/photos/OtXADkUh3-I) | Separate living room with sofa |
| `room-villa.jpg` | [`jetnF4Xv4h8`](https://unsplash.com/photos/jetnF4Xv4h8) | Villa with a private plunge pool and garden |
| `dining-bistro.jpg` | [`TivEEYzzhik`](https://unsplash.com/photos/TivEEYzzhik) | Bright all-day dining room |
| `dining-sky-lounge.jpg` | [`xQWLtlQb7L0`](https://unsplash.com/photos/xQWLtlQb7L0) | Rooftop terrace over the city at sunset |
| `dining-grill.jpg` | [`ZgREXhl8ER0`](https://unsplash.com/photos/ZgREXhl8ER0) | Dimly lit fine-dining room |
| `dining-poolside.jpg` | [`8Jps7JdLzpM`](https://unsplash.com/photos/8Jps7JdLzpM) | Lit poolside bar at night |
| `slide-about.jpg` | [`zSG-kd-L6vw`](https://unsplash.com/photos/zSG-kd-L6vw) | Lobby lounge, wood screens and warm light |
| `slide-policies.jpg` | [`kfnWOD1Tbp8`](https://unsplash.com/photos/kfnWOD1Tbp8) | Reception desk with a member of staff |

## Replacing one

Drop a new file in with the same name. Several of these are portrait shots and every slot
crops to `3 / 2` with `object-fit: cover`, so keep the subject near the centre of the frame —
the top and bottom of a tall photograph will be cut off.
