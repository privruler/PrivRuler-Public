### Privruler: An Automated Tool to Evaluate the Security Risks of Exposed Cloud Services in Mobile Apps
PrivRuler has two key components: 1) AppAnalysis: a static app analysis component that extracts cloud service credentials and cloud service usages from mobile apps. 2) CloudProbe: a component that takes the output of AppAnalysis as input, and probes the corresponding cloud services to infer what extra capabilities are granted to mobile apps. 

For more details, please refer to our manuscript: "Credit Karma: Understanding Security Implications of Exposed Cloud Services through Automated Capability Inference" in [here](https://github.com/privruler/PrivRuler-Public).

#### Prerequisites
There is no special requirements to run the code as long as you have JDK 8+ and Android Studio installed. 

#### Setup
You could follow the following steps to run AppAnalysis.
- Clone this repository, and place it in: $dir/PrivRuler-Public
- Put your apps (apks) into a directory: $apk_dir
- Compile the AppAnalysis component with: cd $dir/PrivRuler-Public/AppAnalysis;./compile.sh
- Run the AppAnalysis component with: ./analyze.sh $apk_dir any_string
  - The output is ready at: $dir/PrivRuler-Public/AppAnalysis/output

And use the following steps to run CloudProbe, once the AppAnalysis result is ready:
- Prepare the input for CloudProbe: grep -Rh 'appName.*cloudAPIs' $dir/PrivRuler-Public/AppAnalysis/output >> summary
- Launch an Android emulator, or connect a physical android device, and ensure adb is connected.
- Prepare the emulator/device with:
  - adb shell mkdir /sdcard/cloudassets
  - adb push summary /sdcard/cloudassets
- import CloudProbe into Android Studio, and run the app.
  - Follow the instructions on the app's UI, and the probe results will be ready in /sdcard/AWSSummaries, /sdcard/AzureSummaries and /sdcard/AlibabaSummaries, depending on which cloud service you plan to test. 

## Disclaimer
The tool is only a prototype for analyzing mobile apps and their cloud service usages. None of the authors is connected in any way with a mobile platform vendor (e.g., Google) or cloud service vendor (e.g., Alibaba, AWS, Azure). The tool is also not effectively peer reviewed, and may lead to unexpected errors or even potential harms to the cloud backends. Please use it at your own risk. 

## Contact
Anonymized (privruler@gmail.com)
