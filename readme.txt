Verbeteren van de gebruikerservaring op mobiele toestellen
______________________© Thomas Planckaert______________________

Weergave van broncode:
______________________
De volledige broncode is verpakt in een Android project. Om dit project te openen in Eclipse is het noodzakelijk om de ADT-plug-in te installeren. Dit kan binnen een bestaande Eclipse-installatie of ook door het downloaden van de volledige Android-SDK inclusief de ADT-bundle.

Deze ADT-bundle is terug te vinden op de volgende link: http://developer.android.com/sdk/index.html
Deze is beschikbaar voor de volgende platformen:
	=> Windows 32/64-bits
	=> Linux 32/64-bits
	=> Mac OS 64-bits
	
Compileren van de broncode:
___________________________
De Eclipse-workspace bevat volgende twee projecten:
1/ TutorialLibrary.
   Dit project bevat de ShowcaseView bibliotheek. Dit is een Android-library project en wordt gebruikt door het tweede project.
2/ TubePLUS.
   Dit project bevat het QoE-framework, vragenlijsten en andere. Door dit project te compileren verkrijg je de eigenlijke applicatie. Bij het verplaatsen van de workspace kan de link met het TutorialLibrary-project een probleem opleveren. Dit kan worden hersteld via:
   'Properties menu van TubePLUS => Android => Library => Add'
   In dit menu kan u het TutorialLibrary-project opnieuw toevoegen. Als er nog steeds problemen voorkomen, kan dit opgelost worden door eerst het Tutorial project te builden en vervolgens TubePLUS te builden.
	
Ondersteunde Android-versies:
_____________________________
De gecompileerde broncode (of het toegevoegd apk-bestand) kan uitgevoerd worden op Android-versies 3.0 (API-level 11) of hoger. De ondersteunde instructiesets zijn: ARMv6, ARM VFP, ARMv7 en ARM NEON. De x86-instructieset wordt niet ondersteund, hierdoor kan de applicatie niet worden uitgevoerd in een emulator die gebruikmaakt van HAXM (Intel® Hardware Accelerated Execution Manager).

Uitvoeren op een fysiek toestel:
________________________________
1) De applicatie is verkrijgbaar op Google Play Store en kan via de volgende link verkregen worden:
https://play.google.com/store/apps/details?id=be.ugent.iii.youtube of door te zoeken op de naam 'TubePLUS'.

2) Ook is het mogelijk de applicatie te installeren via IDE door het te deployen op een fysiek toestel. Hiervoor wordt het project geopend in Eclipse en wordt het vervolgens uitgevoerd als application.
Om de communicatie tussen het fysiek toestel en de computer voor de eerste maal uit te voeren, moeten er mogelijk nog extra drivers geïnstalleerd worden. Deze installatie is afhankelijk van platform tot platform. De Android-documentatie is hierover duidelijk. Hiervoor kunt u volgende pagina raadplegen: http://developer.android.com/tools/device.html.

Voor de communicatie met Eclipse is het ook nodig om de instelling USB-debugging aan te zetten op het fysiek toestel. Dit kan op de volgende manier:
'Instellingen => Opties voor ontwikkelaars => USB-foutopsporing'


3) Een laatste mogelijkheid is het verplaatsen van het bijgeleverde APK bestand naar het toestel en daar installeren. Dit kan vanuit Eclipse (dan wordt de installatie automatisch gestart) of door het bestand te downloaden naar het toestel en daarna handmatig de installatie uit te voeren. Deze handmatige installatie is niets anders dan het apk-bestand via een file-manager applicatie lokaliseren en openen.