# Prompt para o Google Stitch — Comes&Bebes

## Como usar

Cole o **prompt principal**, em inglês, em um novo projeto do Stitch. O primeiro objetivo é gerar o sistema visual e as telas centrais. Depois da primeira geração, use os prompts de refinamento no mesmo projeto, preservando o contexto e o design system.

O formato recomendado para as publicações é `4:5`: mantém a fotografia grande no feed e oferece espaço suficiente para o verso da receita sem transformar a tela em uma faixa horizontal.

---

## Prompt principal

```text
Design a polished, responsive social web application called “Comes&Bebes”.

PRODUCT CONCEPT

Comes&Bebes is a positive, photo-first social network dedicated exclusively to real food photos, dishes, and recipes. It should feel like a warm independent food magazine or Sunday newspaper food section that happens to have social interactions. It must not feel like Instagram, a restaurant delivery app, a recipe marketplace, a fitness or nutrition tracker, or a generic SaaS dashboard.

The emotional goals are warmth, appetite, curiosity, comfort, authenticity, and the pleasure of sharing food without social pressure. Food photography is the main character. Interface elements must support the photos instead of competing with them.

There are no comments, private messages, follower counts, popularity rankings, trending sections, profile photos, or photos of people. Social interaction is intentionally lightweight and positive.

BRAND

Use the exact wordmark “Comes&Bebes”, with this capitalization and no space around the ampersand.

For now, the logo is typographic only. Do not add forks, knives, chef hats, plates, speech bubbles, flames, leaves, or restaurant-style symbols to the logo.

The wordmark should resemble the masthead of a contemporary independent food magazine: memorable, editorial, friendly, and confident. Use a characterful serif typeface such as Fraunces, Libre Baskerville, or a similar typeface with full Portuguese character support. Use a highly readable sans-serif such as Inter or Source Sans 3 for UI controls and body copy.

VISUAL DIRECTION

Create a contemporary editorial design with subtle references to printed magazines, newspapers, vintage food photographs, handwritten recipe cards, and physical paper. It should feel crafted but not nostalgic to the point of looking old-fashioned.

Use generous whitespace, clear hierarchy, large food photography, restrained borders, subtle paper texture, and soft shadows. Corners may be gently rounded, but avoid turning every component into a pill. Avoid excessive gradients, glassmorphism, neon gamer aesthetics, overdecorated cards, glossy food-delivery visuals, and generic dashboard layouts.

LIGHT THEME

- App background: warm cream, approximately #FFF8F4.
- Primary surface: soft paper white, approximately #FFFDFB.
- Primary text: dark aubergine or ink, approximately #281F2B.
- Secondary text: muted warm gray, approximately #756A76.
- Primary brand color: deep plum, approximately #6E3AA8.
- Accent color: editorial rose, approximately #D94880.
- Soft accent background: pale lavender, approximately #EDE1F6.
- Borders: warm neutral, approximately #E8DDE5.
- Error and destructive actions: muted red, clearly distinct from the pink accent.

DARK THEME

Dark mode is a first-class experience, not an afterthought.

- Main background: near-black with a subtle purple undertone, approximately #0D0910.
- Primary surface: approximately #18111D.
- Raised surface: approximately #211629.
- Primary text: warm off-white, approximately #F8F1F8.
- Secondary text: muted lavender-gray, approximately #B9AABA.
- Primary purple: approximately #B27AFF.
- Accent pink: approximately #FF6FAE.
- Borders: approximately #3A2942.

Purple and pink should look sophisticated against black, like an independent culture magazine at night. Do not make dark mode look like a cyberpunk, gaming, nightclub, or streaming interface. Maintain the warmth of food photography and physical paper references.

Support a visible theme switch in the header, respect the system theme by default, and preserve the user’s choice.

ACCESSIBILITY

- Meet WCAG AA contrast requirements in both themes.
- Do not rely only on color to communicate state.
- All interactive elements need visible focus states.
- Target sizes must work comfortably on mobile.
- Respect prefers-reduced-motion.
- Recipe card flipping must also work with keyboard activation and screen readers.
- Provide text labels or accessible names for icon-only actions.

CONTENT MODEL

There are three publication types:

1. DISH: a large food photo. Title and description may be absent. It has no recipe on the back.
2. RECIPE: a food photo connected to a complete recipe with a title, ordered ingredients, and preparation instructions.
3. MY VERSION: a complete recipe inspired by another recipe. Its title preserves the original recipe title as a prefix, for example “Lasanha à bolonhesa — versão com berinjela”. Clearly show a discreet “Inspired by” or “Minha versão de” relationship without making it feel like a repost.

Visibility can be PUBLIC or INTERNAL. Public content is available to visitors. Internal content requires authentication. Visibility should not dominate the feed; show it only where relevant to the author or in creation forms.

REACTIONS

The available positive reactions are:

- “Eu comeria”
- “Quero fazer”
- “Comida afetiva”

A user may select several different reactions, but may apply each reaction only once. Selecting an active reaction again removes it. Reaction controls should feel like small editorial stamps, annotations, or tasteful text buttons rather than generic social-media hearts.

Reaction totals may be visible or hidden according to the publication author’s preference. Never show the identities of people who reacted.

“Fiz também” is not a reaction. It is a distinct action available for recipes that starts the creation of the user’s own derived recipe. Give it a meaningful but restrained visual treatment.

CORE FEED LAYOUT

Use one large publication per row. Do not use a masonry feed or a dense grid for the main feed.

Desktop:

- A fixed or sticky top navigation bar.
- Typographic Comes&Bebes wordmark on the left.
- Prominent but elegant search field near the center.
- Actions on the right: create publication, saved items, notifications, theme toggle, and username/profile menu.
- Main feed centered at a comfortable reading width, approximately 680–760 px.
- Avoid an empty right sidebar or fake trending modules.
- Use generous space between publications, like articles in a magazine.

Mobile:

- Compact header with wordmark, search access, notifications, and theme control.
- Bottom navigation with Home, Search, Publish, Saved, and Profile.
- Keep the food photo visually dominant.
- Do not hide important actions behind hover states.

PUBLICATION CARD — FRONT

Use a 4:5 card format inspired by a physical vintage photograph or a carefully printed magazine plate, but keep it contemporary.

- Large real food photo as the dominant element.
- No people, faces, hands, portraits, or human body parts in example images.
- Prefer authentic home-cooked food, desserts, simple recipes, and occasional elaborate dishes.
- Use natural lighting and realistic imperfections. Avoid overly polished restaurant ads or obvious stock photography.
- A small header above the image may show username, relative publication time, and a subtle type label: “Prato”, “Receita”, or “Minha versão”. There is no user avatar.
- Keep title treatment editorial and secondary to the image. Dish titles may be absent.
- Place reactions outside the flipping photo area so clicking a reaction never flips the card.
- Do not render a comment field, comment count, share-to-DM action, follower information, or generic like button.

SIGNATURE INTERACTION — FLIPPING RECIPE PHOTO

This is the defining interaction of Comes&Bebes.

For RECIPE and MY VERSION publications, clicking or tapping the food photo should perform a tactile 3D flip, as if the user were turning over an old printed photograph and discovering a handwritten recipe on its back.

Front side:

- Food photograph with a subtle paper border.
- A small, elegant affordance such as “Virar para ver a receita” or a rotate-card icon with an accessible label.
- Do not flip on hover. Flip only through deliberate click, tap, Enter, or Space.

Back side:

- A warm recipe-card surface that visually belongs to the same physical object.
- Show recipe title, yield when available, ordered ingredients, and preparation.
- Use subtle ruled-paper, paper grain, margin marks, or restrained handwritten annotations, while keeping the actual recipe text highly readable in a standard typeface.
- Preserve the same width and overall card identity.
- For long recipes, allow a clear internal reading area and provide a prominent “Ver receita completa” action. Do not shrink text to force the entire recipe into the card.
- Include a clear “Voltar para a foto” control.
- On MY VERSION, preserve the original-title prefix and show the source recipe relationship.

For DISH publications, clicking the photo opens the dish detail view; it must not show an empty or fake recipe back.

Motion behavior:

- Use believable perspective, a soft shadow transition, and 350–500 ms duration.
- Preserve spatial orientation and avoid flashy bounce effects.
- Prevent mirrored text during the animation.
- With prefers-reduced-motion, replace the 3D rotation with an instant crossfade or content swap while preserving the same functionality.
- Maintain the flipped state until the user flips it back or navigates away.

SCREENS TO GENERATE IN THIS FIRST DESIGN PASS

Generate a cohesive design system and responsive designs for these screens and states:

1. PUBLIC DESKTOP FEED
   - Visitor can browse PUBLIC and ACTIVE publications.
   - Show a mixture of DISH, RECIPE, and MY VERSION cards.
   - Include one recipe card in its front state and another in its flipped-back state.
   - Provide tasteful sign-in and registration calls to action without blocking browsing.

2. AUTHENTICATED MOBILE FEED
   - Include bottom navigation.
   - Show selected and unselected reaction states.
   - Show the “Fiz também” action on a recipe.
   - Include light and dark theme variants.

3. RECIPE DETAIL PAGE
   - Large photograph.
   - Editorial title and author username without avatar.
   - Ingredients and preparation with strong readability.
   - Positive reactions, saved state, “Fiz também”, and report action.
   - If it is MY VERSION, show the source recipe relationship.
   - No comments or related-person recommendations.

4. CREATE PUBLICATION FLOW
   - Choose between Prato, Receita, and Minha versão.
   - Upload exactly one food image.
   - Clearly communicate that the image cannot be replaced after publication.
   - Choose PUBLIC or INTERNAL visibility with plain-language explanations.
   - Dish title and description are optional.
   - Recipe title, ingredients, and preparation are required.
   - Ingredients are ordered structured rows with name, quantity, unit, and optional note.
   - Preparation is plain text written one step per line.
   - For Minha versão, preserve the original recipe title as a fixed prefix and allow the user to write a required suffix.
   - Include pending image-validation and uncertain-validation states.

5. PUBLIC USER PROFILE
   - No avatar, biography, location, specialties, follower count, or popularity statistics.
   - Show display name, username, and publications.
   - Use the same large single-column publication treatment where practical.

6. COMPONENT AND STATE SHEET
   - Typography scale.
   - Light and dark color tokens.
   - Spacing, radii, borders, shadows, and paper textures.
   - Buttons, fields, dropdowns, visibility controls, reaction states, saved states, notification indicators, publication labels, validation states, empty states, loading skeletons, and focus states.
   - Front and back anatomy of the flipping recipe card.

CONTENT LANGUAGE

All visible product copy must be Brazilian Portuguese. Use natural, friendly language rather than literal translations. Use plausible recipe names and ingredient text. Keep the interface warm but not childish or overly cute.

DESIGN CONSTRAINTS

- No user avatars or profile photos.
- No comments.
- No messages.
- No follower counts.
- No trending or ranking modules.
- No nutrition information in the MVP.
- No category chips, themes, hashtags, or topic filters.
- No multiple-image carousel.
- Do not add features that were not requested.
- Avoid duplicating Instagram, Pinterest, TikTok, delivery apps, or generic recipe-blog layouts.

OUTPUT EXPECTATION

Create a high-fidelity, production-oriented responsive design, not a low-fidelity wireframe. Make the feed and flip interaction feel distinctive enough that the product is recognizable even without the wordmark.

After establishing the visual system, produce or update a DESIGN.md that documents semantic color roles, typography, spacing, component rules, light/dark behavior, accessibility requirements, and the flipping recipe-card interaction so future screens remain consistent.
```

---

## Primeiro refinamento: conferir a identidade da virada

Use este prompt depois que o Stitch gerar a primeira versão:

```text
Focus only on the publication card and its front-to-back recipe interaction.

Make the object feel like a real printed food photograph whose back contains a personal recipe card. Refine the perspective, paper border, shadows, front/back relationship, flip affordance, recipe typography, long-content behavior, keyboard state, mobile behavior, dark theme, and reduced-motion alternative.

Keep reactions outside the flipping area. Do not add comments, avatars, follower information, or generic like icons. Show side-by-side states for DISH, RECIPE, and MY VERSION, including front, flipping transition, back, keyboard focus, and reduced-motion variants.
```

## Segundo refinamento: reduzir aparência genérica

```text
Review the generated design and remove anything that resembles Instagram, Pinterest, a restaurant delivery app, a nutrition tracker, or a generic SaaS dashboard.

Strengthen the independent food-magazine identity through typography, editorial spacing, photographic art direction, subtle print references, and the distinctive recipe-card flip. Keep the experience contemporary, accessible, calm, and photo-first. Preserve all existing product constraints.
```
