# Commands

Base command:

```text
/uniquelegends
```

Aliases:

```text
/ul
/legendarylock
```

Commands require permission level 2.

## Reload

```text
/uniquelegends reload
```

Reloads config and storage.

## List

```text
/uniquelegends list
```

Lists active locks.

## Info

```text
/uniquelegends info <species>
```

Shows the current lock owner for a species.

The species argument uses Cobblemon autocomplete, like `/pokespawn`.

Examples:

```text
/uniquelegends info articuno
/uniquelegends info cobblemon:mewtwo
```

## Unlock

```text
/uniquelegends unlock <species>
```

Manually releases the active lock for a species.

Examples:

```text
/uniquelegends unlock articuno
/uniquelegends unlock cobblemon:articuno
```

What it does:

- finds the lock for the selected species
- marks that lock as inactive
- saves Unique Legends storage
- allows that species to be captured again

What it does not do:

- it does not remove the Pokemon from the current owner
- it does not scan player storages
- it does not check whether the owner still has the Pokemon

Important: if you unlock `Articuno` while a player still owns the registered Articuno, another player can capture a second Articuno. Use this command when you intentionally want to free a species manually.

## Scan

```text
/uniquelegends scan
```

Scans known players and reports tracked Pokemon, duplicates, and stale conditions.

```text
/uniquelegends scan fix
```

Scans known players and fixes what it can:

- registers missing locks
- releases stale locks when the stored Pokemon UUID no longer exists
- updates lock ownership after trades

## Check inactive

```text
/uniquelegends check-inactive
```

Runs inactivity cleanup immediately.

## Debug

```text
/uniquelegends debug
```

Shows basic config and registry status.
