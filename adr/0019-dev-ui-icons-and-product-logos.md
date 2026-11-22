# 19. Dev UI icons and product logos

Date: 2026-11-21

## Status

Accepted

## Context

Many cards of the Dev UI (see [16. Dev UI](0016-dev-ui.md)) are about a product that is not Arconia's: each Dev Service runs one (a database, a message broker, an observability platform), and the modules of an application integrate others. The logo of a product is the quickest way to recognize its card.

A logo is a trademark, and usually also an artwork under copyright. Whether a third party may show it is up to its owner. The trademark and brand policies of the products behind the current Arconia Dev Services fall into four groups:

* Some owners reserve their logo for licensees and partners. Their policies allow the name of the product to be used in text to refer to it, and say in plain words that this doesn't extend to the logo.
* Some ask for permission before the logo is used, or grant no rights to their branding without it.
* Some allow the logo to be used to refer to the product, under conditions. The usual ones are that the official file is used unmodified, without changing its colors, that it links to the website of the project, and that a trademark notice is shown. The policies of the Apache Software Foundation and of the Linux Foundation are of this kind, and both speak of websites, not of a tool that runs on the machine of a developer.
* Some products have no policy, and one has no logo.

Few owners license the artwork itself. Most give, at best, a permission to use the trademark, with nothing that would let the image be redistributed under the license of Arconia. Several permissions can be withdrawn.

Arconia is published to Maven Central, where a release can be neither changed nor removed. A website that receives an objection takes the image down. A library can only leave it out of its next release, and every earlier release keeps distributing it.

Several ways forward were considered. Bundling every logo, as files owned by each module, ignores the policies that reserve them. Bundling only the logos whose policy allows it, with a trademark notice and a record of where each file comes from, is defensible, but leaves the Dev UI with logos on some cards and generic icons on others, and still puts into permanent releases artwork whose terms the owner can change. Asking each owner for permission in writing is the proper way to get a logo, and takes time for each one.

## Decision

Arconia bundles no logo of a product it doesn't own, in the Dev UI or anywhere else in its artifacts.

Every card is identified by the name of its product, in text, and by an icon from the Lucide icon set, which the Dev UI already uses for everything else (see [17. Dev UI frontend](0017-dev-ui-frontend.md)). The icon is chosen for what the product is, such as a database or a message broker, or for what its name evokes, such as a rabbit or a telescope. It is always the icon as the icon set draws it, in the colors of the Dev UI: it is never given the colors of a brand or redrawn to look like the mark of an owner.

## Consequences

A card is a little less recognizable at a glance than it would be with a logo, and products of the same kind share an icon: the relational databases all show the same one. The title of the card is what tells them apart.

There is nothing to keep track of: no trademark notices, no record of where files come from and under which terms, no versions for light and dark backgrounds, and no file to remove when an owner changes its policy.

A library outside Arconia that contributes a card cannot show its own logo either, even though it could rightfully do so, because the API has no way to provide one, at the moment.

The decision can be revisited for a product whose owner gives permission in writing, or publishes terms that allow its logo to be redistributed. A way to provide an image would then be added to the API, with the files owned by the module that integrates the product, as the Dev Services own their icons today.
