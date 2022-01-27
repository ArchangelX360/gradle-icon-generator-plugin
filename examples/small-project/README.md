# `small-project`

`small-project` is made for checking the correctness of the plugin, answering questions such as:

- is the plugin generating the icons in the right folder?
- is the plugin creating/updating/deleting the expected icons when we manually change the source files?
- how does the plugin handle "situation X"?

## Run the example

First, clone the repository, if not already done:
```
git clone https://github.com/ArchangelX360/gradle-icon-generator-plugin.git
cd gradle-icon-generator-plugin
```

Second, go to the example directory:
```
cd examples/small-project/
```

Then, `small-project` being already generated, you can simply run:

```
# (optional) cleanup any previously generated outputs
./gradlew cleanIcons

# Run the plugin's icon generation
./gradlew generateIcons
```

You should see some parsing errors, some ignored field warnings. Indeed, the example is set up with some ignored/invalid
files on purpose. Feel free to modify the sources, delete some files and re-run the command to see the cleanup/update in
action!
