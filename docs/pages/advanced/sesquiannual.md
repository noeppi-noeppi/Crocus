# Sesquiannual

Sesquiannual is a library for dealing with iCalendar data that extends the
 [Biweekly](https://github.com/mangstadt/biweekly) library.
Its main feature is the ability to calculate when individual events take place, taking into account recurrence rules and
 specific instances of repetition.

Sesquiannual is developed alongside Crocus and is used in the [iCal Plugin](../plugins/ical.md).
However, as it may be useful on its own, it is also available as a standalone library.
It is published on the [tuxtown maven](https://maven.tuxtown.eu/release).

The following example shows how to include Sesquiannual in a Gradle project.

```groovy title="build.gradle"
repositories {
    maven { url = 'https://maven.tuxtown.eu/release' }
}

dependencies {
    api 'eu.tuxtown.crocus:sesquiannual:<version>'
}
```

!!! quote "[xkcd/1602 - Linguistics Club](https://xkcd.com/1602/)"

    ![](https://imgs.xkcd.com/comics/linguistics_club_2x.png)

    xkcd by Randall Munroe - [https://xkcd.com](https://xkcd.com/)
