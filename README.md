# PlotCraft
A tool for creating Minecraft layouts

I got tired of using programs that weren't designed for it to draw out plans for areas in Minecraft; and all the tools I managed to find to do it were slow, buggy, unintuitive, or otherwise unpleasant to use. So I decided to use my vacation time to put together my own tool. You are welcome to use it, modify it, and improve it. I'll keep plinking away at it as I have time, I'm sure. Lots of ideas for this one. But for now, it's at least an editor that can save, load, draw, that responds quickly and doesn't seem to blow my RAM budget or choke up on large plots. ;)

## Gradle ##

The project has been reconfigured to use the Gradle application plugin. In order to create a distributable zip file, run `gradle zipDist` from the project root directory.

Please let me know if you the setup is busted.

## Notes ##

### Textures ###

The current projects assumes that there is a "textures/blocks" directory located in the root directory, which it copies to the `/bin` folder. This is where the program looks for tile images (specified in settings.json). I will not include the textures from Minecraft without permission, so if you want them, you should get them yourself. It's not that difficult, since you probably already have a copy of Minecraft.

Instructions for copying your Minecraft textures directory:

* TODO

Eventually I will create some free tiles that I feel comfortable distributing with the project and compiled executable.
