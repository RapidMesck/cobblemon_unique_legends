# Admin Workflows

## Installing on an existing server

1. Install the mod.
2. Start the server.
3. Let startup scan run, or run:

```text
/uniquelegends scan fix
```

4. Check active locks:

```text
/uniquelegends list
```

## Manually freeing a species

Use:

```text
/uniquelegends unlock <species>
```

Example:

```text
/uniquelegends unlock articuno
```

This only releases the Unique Legends lock. It does not remove the Pokemon from the current owner.

If the current owner still has the Pokemon, unlocking the species allows another player to capture another one.

## Fixing stale locks

If a species is locked but the Pokemon no longer exists:

```text
/uniquelegends scan fix
```

The command releases stale locks when the stored Pokemon UUID is not found in known player party or PC data.

## Fixing trade ownership

If ownership is wrong after a trade:

```text
/uniquelegends scan fix
```

The command updates the lock owner when it finds the same Pokemon UUID in another player's storage.

## Checking inactive owners now

Use:

```text
/uniquelegends check-inactive
```

This is useful before a scheduled event or after changing inactivity settings.

## Debugging config status

Use:

```text
/uniquelegends debug
```
